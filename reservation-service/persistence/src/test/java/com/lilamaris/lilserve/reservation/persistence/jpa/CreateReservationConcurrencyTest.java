package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.application.service.CreateReservationService;
import com.lilamaris.lilserve.reservation.application.service.UpdateRoomService;
import com.lilamaris.lilserve.reservation.application.service.UpdateScheduleService;
import com.lilamaris.lilserve.reservation.application.service.UpdateSeatService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies reservation/resource races using real PostgreSQL locks and persistence adapters. Like
 * the cancellation transaction tests, each worker owns its transaction and pauses at a controlled
 * boundary. Database blocking relationships establish that the competing statement actually waits.
 */
@DisplayName("예약 생성과 자원 변경의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    RoomJpaAdapter.class,
    SeatJpaAdapter.class,
    ScheduleJpaAdapter.class,
    ReservationJpaAdapter.class,
    ReservedScheduleJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CreateReservationConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final LocalDate DATE = LocalDate.of(2026, 11, 9);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private RoomJpaAdapter roomAdapter;
    @Autowired private SeatJpaAdapter seatAdapter;
    @Autowired private ScheduleJpaAdapter scheduleAdapter;
    @Autowired private ReservationJpaAdapter reservationAdapter;
    @Autowired private ReservedScheduleJpaAdapter allocationAdapter;

    private final RoomId roomId = RoomId.of(UUID.randomUUID());
    private final SeatId seatId = SeatId.of(UUID.randomUUID());
    private final ScheduleId morning = ScheduleId.of(UUID.randomUUID());
    private final ScheduleId afternoon = ScheduleId.of(UUID.randomUUID());

    @BeforeEach
    void commitsActiveResources() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
                            persistSchedule(morning, 10);
                            persistSchedule(afternoon, 14);
                        });
    }

    @AfterEach
    void deletesOnlyThisTestsCommittedRows() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            for (var table :
                                    List.of(
                                            "reserved_schedule",
                                            "reservation",
                                            "schedule",
                                            "seat")) {
                                entityManager
                                        .createNativeQuery(
                                                "delete from " + table + " where room_id = :roomId")
                                        .setParameter("roomId", roomId.getValue())
                                        .executeUpdate();
                            }
                            entityManager
                                    .createNativeQuery("delete from room where id = :roomId")
                                    .setParameter("roomId", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    @DisplayName("예약이 자원을 먼저 잠그면 자원 삭제는 예약 커밋까지 대기한다")
    void waitsForReservationCommitBeforeDeletingResource(Resource resource) throws Exception {
        var locked = new CountDownLatch(1);
        var releaseReservation = new CountDownLatch(1);
        var deletionStarted = new CountDownLatch(1);
        var reservationPid = new AtomicInteger();
        var deletionPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var reservation =
                    executor.submit(
                            () ->
                                    reserve(
                                            seatId,
                                            () -> reservationPid.set(backendPid()),
                                            () -> {
                                                locked.countDown();
                                                await(releaseReservation);
                                            }));
            await(locked);
            var deletion =
                    executor.submit(
                            () ->
                                    deleteInTransaction(
                                            resource,
                                            signalStarted(deletionPid, deletionStarted),
                                            () -> {},
                                            false));
            await(deletionStarted);
            awaitBlockedBy(deletionPid.get(), reservationPid.get());
            releaseReservation.countDown();

            assertThat(reservation.get(20, TimeUnit.SECONDS).scheduleIds())
                    .containsExactlyInAnyOrder(morning, afternoon);
            deletion.get(20, TimeUnit.SECONDS);
            assertRowCounts(1, 2);
            assertDeleted(resource, true);
        } finally {
            releaseReservation.countDown();
            shutdown(executor);
        }
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    @DisplayName("자원 삭제가 먼저 확정되면 대기한 예약을 거부하고 헤더와 점유를 남기지 않는다")
    void rejectsReservationAfterResourceDeletionCommits(Resource resource) throws Exception {
        var deleted = new CountDownLatch(1);
        var releaseDeletion = new CountDownLatch(1);
        var reservationStarted = new CountDownLatch(1);
        var deletionPid = new AtomicInteger();
        var reservationPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var deletion =
                    executor.submit(
                            () ->
                                    deleteInTransaction(
                                            resource,
                                            () -> deletionPid.set(backendPid()),
                                            () -> {
                                                deleted.countDown();
                                                await(releaseDeletion);
                                            },
                                            false));
            await(deleted);
            var reservation =
                    executor.submit(
                            () ->
                                    assertRejectedReservation(
                                            resource,
                                            signalStarted(reservationPid, reservationStarted)));
            await(reservationStarted);
            awaitBlockedBy(reservationPid.get(), deletionPid.get());
            releaseDeletion.countDown();

            deletion.get(20, TimeUnit.SECONDS);
            reservation.get(20, TimeUnit.SECONDS);
            assertRowCounts(0, 0);
            assertDeleted(resource, true);
        } finally {
            releaseDeletion.countDown();
            shutdown(executor);
        }
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    @DisplayName("선행 자원 삭제가 롤백되면 대기한 예약은 활성 자원으로 정상 생성된다")
    void createsReservationAfterResourceDeletionRollsBack(Resource resource) throws Exception {
        var deleted = new CountDownLatch(1);
        var releaseDeletion = new CountDownLatch(1);
        var reservationStarted = new CountDownLatch(1);
        var deletionPid = new AtomicInteger();
        var reservationPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var deletion =
                    executor.submit(
                            () ->
                                    deleteInTransaction(
                                            resource,
                                            () -> deletionPid.set(backendPid()),
                                            () -> {
                                                deleted.countDown();
                                                await(releaseDeletion);
                                            },
                                            true));
            await(deleted);
            var reservation =
                    executor.submit(
                            () ->
                                    reserve(
                                            seatId,
                                            signalStarted(reservationPid, reservationStarted),
                                            () -> {}));
            await(reservationStarted);
            awaitBlockedBy(reservationPid.get(), deletionPid.get());
            releaseDeletion.countDown();

            deletion.get(20, TimeUnit.SECONDS);
            assertThat(reservation.get(20, TimeUnit.SECONDS).scheduleIds())
                    .containsExactlyInAnyOrder(morning, afternoon);
            assertRowCounts(1, 2);
            assertDeleted(resource, false);
        } finally {
            releaseDeletion.countDown();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("서로 다른 좌석의 예약은 같은 방과 스케줄의 공유 락을 동시에 획득한다")
    void allowsConcurrentReservationsToShareRoomAndSchedules() throws Exception {
        var otherSeat = SeatId.of(UUID.randomUUID());
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager.persist(
                                        Seat.of(otherSeat, roomId, "SEAT-02", CREATED_AT)));
        var locked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first =
                    executor.submit(
                            () ->
                                    reserve(
                                            seatId,
                                            () -> {},
                                            () -> {
                                                locked.countDown();
                                                await(releaseFirst);
                                            }));
            await(locked);
            var second = executor.submit(() -> reserve(otherSeat, () -> {}, () -> {}));

            // The second reservation must commit while the first still owns every resource lock.
            assertThat(second.get(5, TimeUnit.SECONDS).seatId()).isEqualTo(otherSeat);
            releaseFirst.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS).seatId()).isEqualTo(seatId);
            assertRowCounts(2, 4);
        } finally {
            releaseFirst.countDown();
            shutdown(executor);
        }
    }

    private CreateReservationResult reserve(
            SeatId seat, Runnable beforeCreate, Runnable afterLocks) {
        return transaction()
                .execute(
                        status -> {
                            beforeCreate.run();
                            return creation(afterLocks).create(command(seat));
                        });
    }

    private void assertRejectedReservation(Resource resource, Runnable beforeCreate) {
        assertThatThrownBy(() -> reserve(seatId, beforeCreate, () -> {}))
                .isInstanceOf(
                        resource.isSchedule()
                                ? IllegalStateException.class
                                : NoSuchElementException.class);
    }

    private void deleteInTransaction(
            Resource resource, Runnable beforeDelete, Runnable afterFlush, boolean rollback) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            beforeDelete.run();
                            deleteResource(resource);
                            entityManager.flush();
                            afterFlush.run();
                            if (rollback) status.setRollbackOnly();
                        });
    }

    private Runnable signalStarted(AtomicInteger pid, CountDownLatch started) {
        return () -> {
            pid.set(backendPid());
            started.countDown();
        };
    }

    private CreateReservationService creation(Runnable afterLocks) {
        // The worker owns the transaction; this hook pauses only after the real adapters lock rows.
        return new CreateReservationService(
                reservationAdapter,
                allocationAdapter,
                roomAdapter,
                seatAdapter,
                ids -> {
                    var lockedIds = scheduleAdapter.findAllLockedById(ids);
                    afterLocks.run();
                    return lockedIds;
                },
                UUID::randomUUID,
                CLOCK);
    }

    private void deleteResource(Resource resource) {
        switch (resource) {
            case ROOM ->
                    new UpdateRoomService(roomAdapter, roomAdapter, CLOCK)
                            .delete(DeleteRoomCommand.of(roomId));
            case SEAT ->
                    new UpdateSeatService(seatAdapter, seatAdapter, CLOCK)
                            .delete(DeleteSeatCommand.of(seatId));
            case MORNING, AFTERNOON ->
                    new UpdateScheduleService(
                                    scheduleAdapter,
                                    scheduleAdapter,
                                    scheduleAdapter,
                                    UUID::randomUUID,
                                    CLOCK)
                            .delete(resource == Resource.MORNING ? morning : afternoon);
        }
    }

    private void assertDeleted(Resource resource, boolean deleted) {
        var table =
                resource.isSchedule()
                        ? "schedule"
                        : resource.name().toLowerCase(java.util.Locale.ROOT);
        var id =
                switch (resource) {
                    case ROOM -> roomId.getValue();
                    case SEAT -> seatId.getValue();
                    case MORNING -> morning.getValue();
                    case AFTERNOON -> afternoon.getValue();
                };
        var actual =
                transaction()
                        .execute(
                                status ->
                                        entityManager
                                                .createNativeQuery(
                                                        "select deleted_at is not null from "
                                                                + table
                                                                + " where id = :id",
                                                        Boolean.class)
                                                .setParameter("id", id)
                                                .getSingleResult());
        assertThat(actual).isEqualTo(deleted);
    }

    private void assertRowCounts(long headers, long allocations) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            assertThat(countRows("reservation")).isEqualTo(headers);
                            assertThat(countRows("reserved_schedule")).isEqualTo(allocations);
                        });
    }

    private long countRows(String table) {
        return ((Number)
                        entityManager
                                .createNativeQuery(
                                        "select count(*) from "
                                                + table
                                                + " where room_id = :roomId")
                                .setParameter("roomId", roomId.getValue())
                                .getSingleResult())
                .longValue();
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

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(
                    "Interrupted while waiting for transaction boundary", exception);
        }
    }

    private void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
    }

    private void persistSchedule(ScheduleId id, int startHour) {
        entityManager.persist(
                Schedule.of(
                        id,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(startHour, 0),
                        LocalTime.of(startHour + 1, 0),
                        CREATED_AT));
    }

    private CreateReservationCommand command(SeatId seat) {
        return CreateReservationCommand.of(roomId, seat, Set.of(morning, afternoon), DATE);
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private enum Resource {
        ROOM,
        SEAT,
        MORNING,
        AFTERNOON;

        boolean isSchedule() {
            return this == MORNING || this == AFTERNOON;
        }
    }
}
