package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.service.CreateScheduleService;
import com.lilamaris.lilserve.reservation.application.service.UpdateRoomService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies schedule creation and room deletion using PostgreSQL row locks and real adapters. */
@DisplayName("스케줄 생성과 부모 룸 삭제의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    RoomJpaAdapter.class,
    ScheduleJpaAdapter.class,
    CreateScheduleConcurrencyTest.Configuration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CreateScheduleConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalTimeRange MORNING =
            LocalTimeRange.of(LocalTime.of(10, 0), LocalTime.of(11, 0));
    private static final LocalTimeRange AFTERNOON =
            LocalTimeRange.of(LocalTime.of(14, 0), LocalTime.of(15, 0));

    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final RoomJpaAdapter roomAdapter;
    private final ScheduleJpaAdapter scheduleAdapter;
    private final UpdateRoomService deletionService;
    private final CreateScheduleService creationService;
    private final CreateScheduleService failingService;
    private final RoomId roomId = RoomId.of(UUID.randomUUID());

    @Autowired
    CreateScheduleConcurrencyTest(
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            RoomJpaAdapter roomAdapter,
            ScheduleJpaAdapter scheduleAdapter,
            UpdateRoomService deletionService,
            @Qualifier("creationService") CreateScheduleService creationService,
            @Qualifier("failingService") CreateScheduleService failingService) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.roomAdapter = roomAdapter;
        this.scheduleAdapter = scheduleAdapter;
        this.deletionService = deletionService;
        this.creationService = creationService;
        this.failingService = failingService;
    }

    @BeforeEach
    void commitsActiveRoom() {
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager.persist(
                                        Room.of(
                                                roomId,
                                                "ROOM-01",
                                                "hall",
                                                "test room",
                                                CREATED_AT)));
    }

    @AfterEach
    void deletesOnlyThisTestsCommittedRows() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager
                                    .createNativeQuery(
                                            "delete from schedule where room_id = :roomId")
                                    .setParameter("roomId", roomId.getValue())
                                    .executeUpdate();
                            entityManager
                                    .createNativeQuery("delete from room where id = :roomId")
                                    .setParameter("roomId", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @ParameterizedTest
    @EnumSource(Creation.class)
    @DisplayName("스케줄 생성이 먼저 잠그면 룸 삭제는 생성 트랜잭션의 커밋까지 대기한다")
    void waitsForCreationCommitBeforeDeletingRoom(Creation creation) throws Exception {
        var roomLocked = new CountDownLatch(1);
        var releaseCreation = new CountDownLatch(1);
        var deletionStarted = new CountDownLatch(1);
        var creationPid = new AtomicInteger();
        var deletionPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        var pausedService =
                new CreateScheduleService(
                        scheduleAdapter,
                        roomAdapter,
                        () -> {
                            // Pause after the room lock and before any insert can acquire an FK
                            // lock.
                            roomLocked.countDown();
                            await(releaseCreation);
                            return UUID.randomUUID();
                        },
                        CLOCK);
        try {
            var created =
                    executor.submit(
                            () ->
                                    transaction()
                                            .execute(
                                                    status -> {
                                                        creationPid.set(backendPid());
                                                        return create(pausedService, creation);
                                                    }));
            await(roomLocked);
            var deleted =
                    executor.submit(
                            () ->
                                    transaction()
                                            .executeWithoutResult(
                                                    status -> {
                                                        deletionPid.set(backendPid());
                                                        deletionStarted.countDown();
                                                        deletionService.delete(
                                                                DeleteRoomCommand.of(roomId));
                                                    }));
            await(deletionStarted);
            awaitBlockedBy(deletionPid.get(), creationPid.get());
            releaseCreation.countDown();

            var result = created.get(20, TimeUnit.SECONDS);
            deleted.get(20, TimeUnit.SECONDS);
            assertStoredSchedules(creation, result);
            assertRoomDeleted(true);
        } finally {
            releaseCreation.countDown();
            shutdown(executor);
        }
    }

    @ParameterizedTest
    @EnumSource(Creation.class)
    @DisplayName("룸 삭제가 먼저 확정되면 대기한 스케줄 생성을 거부하고 신규 행을 남기지 않는다")
    void rejectsCreationAfterRoomDeletionCommits(Creation creation) throws Exception {
        raceWithDeletion(creation, false);

        assertThat(storedSchedules()).isEmpty();
        assertRoomDeleted(true);
    }

    @ParameterizedTest
    @EnumSource(Creation.class)
    @DisplayName("선행 룸 삭제가 롤백되면 대기한 스케줄 생성은 활성 상태를 확인하고 성공한다")
    void createsSchedulesAfterRoomDeletionRollsBack(Creation creation) throws Exception {
        var result = raceWithDeletion(creation, true);

        assertStoredSchedules(creation, result);
        assertRoomDeleted(false);
    }

    @ParameterizedTest
    @EnumSource(Creation.class)
    @DisplayName("스케줄 저장 도중 실패하면 서비스 트랜잭션은 이미 플러시한 행까지 모두 롤백한다")
    void rollsBackFlushedSchedulesOnFailure(Creation creation) {
        assertThatThrownBy(() -> create(failingService, creation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");

        assertThat(storedSchedules()).isEmpty();
        assertRoomDeleted(false);
    }

    private List<CreateScheduleResult> raceWithDeletion(Creation creation, boolean rollback)
            throws Exception {
        var roomDeleted = new CountDownLatch(1);
        var releaseDeletion = new CountDownLatch(1);
        var creationStarted = new CountDownLatch(1);
        var deletionPid = new AtomicInteger();
        var creationPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var deleted =
                    executor.submit(
                            () ->
                                    transaction()
                                            .executeWithoutResult(
                                                    status -> {
                                                        deletionPid.set(backendPid());
                                                        deletionService.delete(
                                                                DeleteRoomCommand.of(roomId));
                                                        entityManager.flush();
                                                        roomDeleted.countDown();
                                                        await(releaseDeletion);
                                                        if (rollback) status.setRollbackOnly();
                                                    }));
            await(roomDeleted);
            var created =
                    executor.submit(
                            () -> {
                                Supplier<List<CreateScheduleResult>> request =
                                        () ->
                                                transaction()
                                                        .execute(
                                                                status -> {
                                                                    creationPid.set(backendPid());
                                                                    creationStarted.countDown();
                                                                    return create(
                                                                            creationService,
                                                                            creation);
                                                                });
                                if (rollback) return request.get();
                                assertThatThrownBy(request::get)
                                        .isInstanceOf(NoSuchElementException.class);
                                return List.<CreateScheduleResult>of();
                            });
            await(creationStarted);
            awaitBlockedBy(creationPid.get(), deletionPid.get());
            releaseDeletion.countDown();
            deleted.get(20, TimeUnit.SECONDS);
            return created.get(20, TimeUnit.SECONDS);
        } finally {
            releaseDeletion.countDown();
            shutdown(executor);
        }
    }

    private List<CreateScheduleResult> create(CreateScheduleService service, Creation creation) {
        return switch (creation) {
            case SINGLE ->
                    List.of(
                            service.create(
                                    CreateScheduleCommand.of(
                                            roomId,
                                            MORNING.startAt(),
                                            MORNING.endAt(),
                                            DayOfWeek.MONDAY)));
            case BATCH ->
                    service.create(
                                    CreateSchedulesCommand.of(
                                            roomId,
                                            Map.of(DayOfWeek.MONDAY, List.of(MORNING, AFTERNOON))))
                            .schedules();
        };
    }

    private List<CreateScheduleResult> storedSchedules() {
        return transaction()
                .execute(
                        status ->
                                entityManager
                                        .createQuery(
                                                "select s from Schedule s where s.roomId = :roomId",
                                                Schedule.class)
                                        .setParameter("roomId", roomId)
                                        .getResultList()
                                        .stream()
                                        .map(CreateScheduleResult::from)
                                        .toList());
    }

    private void assertStoredSchedules(Creation creation, List<CreateScheduleResult> result) {
        assertThat(result).hasSize(creation == Creation.SINGLE ? 1 : 2);
        assertThat(result)
                .allSatisfy(
                        schedule -> {
                            assertThat(schedule.roomId()).isEqualTo(roomId);
                            assertThat(schedule.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                            assertThat(schedule.createdAt()).isEqualTo(NOW);
                        });
        assertThat(storedSchedules()).containsExactlyInAnyOrderElementsOf(result);
    }

    private void assertRoomDeleted(boolean expected) {
        var deleted =
                transaction()
                        .execute(status -> roomAdapter.findById(roomId).orElseThrow().isDeleted());
        assertThat(deleted).isEqualTo(expected);
    }

    private int backendPid() {
        return ((Number)
                        entityManager
                                .createNativeQuery("select pg_backend_pid()")
                                .getSingleResult())
                .intValue();
    }

    private void awaitBlockedBy(int blockedPid, int blockerPid) {
        var deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        do {
            var blocked =
                    transaction()
                            .execute(
                                    status ->
                                            entityManager
                                                    .createNativeQuery(
                                                            "select :blocker = any(pg_blocking_pids(:blocked))",
                                                            Boolean.class)
                                                    .setParameter("blocker", blockerPid)
                                                    .setParameter("blocked", blockedPid)
                                                    .getSingleResult());
            if (Boolean.TRUE.equals(blocked)) return;
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        } while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted());
        throw new AssertionError("Expected backend " + blockedPid + " to wait for " + blockerPid);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(
                    "Interrupted while waiting for transaction boundary", exception);
        }
    }

    private static void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private enum Creation {
        SINGLE,
        BATCH
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        UpdateRoomService deletionService(RoomJpaAdapter adapter) {
            return new UpdateRoomService(adapter, adapter, CLOCK);
        }

        @Bean
        CreateScheduleService creationService(RoomJpaAdapter rooms, ScheduleJpaAdapter schedules) {
            return new CreateScheduleService(schedules, rooms, UUID::randomUUID, CLOCK);
        }

        @Bean
        CreateScheduleService failingService(
                RoomJpaAdapter rooms, ScheduleJpaAdapter schedules, EntityManager entityManager) {
            var failingSavePort =
                    new ScheduleSavePort() {
                        @Override
                        public void save(Schedule schedule) {
                            schedules.save(schedule);
                            entityManager.flush();
                            throw new IllegalStateException("Storage failed after flush");
                        }

                        @Override
                        public void saveAll(Collection<Schedule> batch) {
                            // Fail after persisting part of the batch to verify atomic rollback.
                            save(batch.iterator().next());
                        }
                    };
            return new CreateScheduleService(failingSavePort, rooms, UUID::randomUUID, CLOCK);
        }
    }
}
