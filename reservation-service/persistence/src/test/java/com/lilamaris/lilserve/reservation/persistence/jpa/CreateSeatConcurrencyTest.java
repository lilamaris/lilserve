package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.service.CreateSeatService;
import com.lilamaris.lilserve.reservation.application.service.UpdateRoomService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import org.junit.jupiter.api.Test;
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

/** Verifies seat creation and room deletion using PostgreSQL row locks and real adapters. */
@DisplayName("좌석 생성과 부모 룸 삭제의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({RoomJpaAdapter.class, SeatJpaAdapter.class, CreateSeatConcurrencyTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CreateSeatConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String CODE = "SEAT-01";

    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final RoomJpaAdapter roomAdapter;
    private final SeatJpaAdapter seatAdapter;
    private final UpdateRoomService deletionService;
    private final CreateSeatService creationService;
    private final CreateSeatService failingService;
    private final RoomId roomId = RoomId.of(UUID.randomUUID());

    @Autowired
    CreateSeatConcurrencyTest(
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            RoomJpaAdapter roomAdapter,
            SeatJpaAdapter seatAdapter,
            UpdateRoomService deletionService,
            @Qualifier("creationService") CreateSeatService creationService,
            @Qualifier("failingService") CreateSeatService failingService) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.roomAdapter = roomAdapter;
        this.seatAdapter = seatAdapter;
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
                                    .createNativeQuery("delete from seat where room_id = :roomId")
                                    .setParameter("roomId", roomId.getValue())
                                    .executeUpdate();
                            entityManager
                                    .createNativeQuery("delete from room where id = :roomId")
                                    .setParameter("roomId", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @Test
    @DisplayName("좌석 생성이 먼저 잠그면 룸 삭제는 생성 트랜잭션의 커밋까지 대기한다")
    void waitsForCreationCommitBeforeDeletingRoom() throws Exception {
        var roomLocked = new CountDownLatch(1);
        var releaseCreation = new CountDownLatch(1);
        var deletionStarted = new CountDownLatch(1);
        var creationPid = new AtomicInteger();
        var deletionPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        var pausedService =
                new CreateSeatService(
                        seatAdapter,
                        roomAdapter,
                        () -> {
                            // Pause after the shared read and before any insert can acquire an FK
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
                                                        return pausedService.create(command());
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
            assertStoredSeat(result);
            assertRoomDeleted(true);
        } finally {
            releaseCreation.countDown();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("룸 삭제가 먼저 확정되면 대기한 좌석 생성을 거부하고 신규 행을 남기지 않는다")
    void rejectsCreationAfterRoomDeletionCommits() throws Exception {
        var result = raceWithDeletion(false);

        assertThat(result).isEmpty();
        assertThat(storedSeats()).isEmpty();
        assertRoomDeleted(true);
    }

    @Test
    @DisplayName("선행 룸 삭제가 롤백되면 대기한 좌석 생성은 활성 상태를 확인하고 성공한다")
    void createsSeatAfterRoomDeletionRollsBack() throws Exception {
        var result = raceWithDeletion(true).orElseThrow();

        assertStoredSeat(result);
        assertRoomDeleted(false);
    }

    @Test
    @DisplayName("좌석 저장과 플러시 후 예외가 발생하면 서비스 트랜잭션은 생성 행을 롤백한다")
    void rollsBackFlushedSeatOnFailure() {
        assertThatThrownBy(() -> failingService.create(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");

        assertThat(storedSeats()).isEmpty();
        assertRoomDeleted(false);
    }

    private Optional<CreateSeatResult> raceWithDeletion(boolean rollback) throws Exception {
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
                                Supplier<CreateSeatResult> request =
                                        () ->
                                                transaction()
                                                        .execute(
                                                                status -> {
                                                                    creationPid.set(backendPid());
                                                                    creationStarted.countDown();
                                                                    return creationService.create(
                                                                            command());
                                                                });
                                if (rollback) return Optional.of(request.get());
                                assertThatThrownBy(request::get)
                                        .isInstanceOf(NoSuchElementException.class);
                                return Optional.<CreateSeatResult>empty();
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

    private CreateSeatCommand command() {
        return CreateSeatCommand.of(roomId, CODE);
    }

    private List<CreateSeatResult> storedSeats() {
        return transaction()
                .execute(
                        status ->
                                entityManager
                                        .createQuery(
                                                "select s from Seat s where s.roomId = :roomId",
                                                Seat.class)
                                        .setParameter("roomId", roomId)
                                        .getResultList()
                                        .stream()
                                        .map(CreateSeatResult::from)
                                        .toList());
    }

    private void assertStoredSeat(CreateSeatResult result) {
        assertThat(result.roomId()).isEqualTo(roomId);
        assertThat(result.code()).isEqualTo(CODE);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(storedSeats()).containsExactly(result);
        transaction()
                .executeWithoutResult(
                        status -> {
                            var saved = seatAdapter.findById(result.seatId()).orElseThrow();
                            assertThat(saved.getUpdatedAt()).isNull();
                            assertThat(saved.getDeletedAt()).isNull();
                        });
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

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        UpdateRoomService deletionService(RoomJpaAdapter rooms) {
            return new UpdateRoomService(rooms, rooms, CLOCK);
        }

        @Bean
        CreateSeatService creationService(RoomJpaAdapter rooms, SeatJpaAdapter seats) {
            return new CreateSeatService(seats, rooms, UUID::randomUUID, CLOCK);
        }

        @Bean
        CreateSeatService failingService(
                RoomJpaAdapter rooms, SeatJpaAdapter seats, EntityManager entityManager) {
            return new CreateSeatService(
                    seat -> {
                        seats.save(seat);
                        entityManager.flush();
                        throw new IllegalStateException("Storage failed after flush");
                    },
                    rooms,
                    UUID::randomUUID,
                    CLOCK);
        }
    }
}
