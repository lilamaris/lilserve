package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.service.CreateBlackoutScheduleService;
import com.lilamaris.lilserve.reservation.application.service.CreateReservationService;
import com.lilamaris.lilserve.reservation.application.service.UpdateBlackoutScheduleService;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
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

/** Verifies room usage decisions across competing blackout transactions using real locks. */
@DisplayName("예약 이용 구간과 스케줄 차단 변경의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    RoomJpaAdapter.class,
    SeatJpaAdapter.class,
    ScheduleJpaAdapter.class,
    ReservationJpaAdapter.class,
    ReservedScheduleJpaAdapter.class,
    BlackoutScheduleJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationBlackoutConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private RoomJpaAdapter roomAdapter;
    @Autowired private SeatJpaAdapter seatAdapter;
    @Autowired private ScheduleJpaAdapter scheduleAdapter;
    @Autowired private ReservationJpaAdapter reservationAdapter;
    @Autowired private ReservedScheduleJpaAdapter allocationAdapter;
    @Autowired private BlackoutScheduleJpaAdapter blackoutAdapter;
    private final RoomId roomId = RoomId.of(UUID.randomUUID());
    private final SeatId seatId = SeatId.of(UUID.randomUUID());
    private final ScheduleId morning = ScheduleId.of(UUID.randomUUID());
    private final ScheduleId afternoon = ScheduleId.of(UUID.randomUUID());
    private final BlackoutScheduleId blackoutId = BlackoutScheduleId.of(UUID.randomUUID());

    @BeforeEach
    void commitsResources() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "", CREATED_AT));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
                            persistSchedule(morning, 10);
                            persistSchedule(afternoon, 14);
                        });
    }

    @AfterEach
    void removesCommittedFixtures() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            for (var table :
                                    List.of(
                                            "reserved_schedule",
                                            "reservation",
                                            "blackout_schedule",
                                            "schedule",
                                            "seat")) {
                                entityManager
                                        .createNativeQuery(
                                                "DELETE FROM " + table + " WHERE room_id = :id")
                                        .setParameter("id", roomId.getValue())
                                        .executeUpdate();
                            }
                            entityManager
                                    .createNativeQuery("DELETE FROM room WHERE id = :id")
                                    .setParameter("id", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("차단 변경이 먼저 커밋되면 대기한 예약은 확정된 차단 상태로 이용 가능 여부를 결정한다")
    void observesCommittedBlackoutChange(Change change) throws Exception {
        raceWithBlackoutFirst(change, false);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("차단 변경이 롤백되면 대기한 예약은 기존 차단 상태로 이용 가능 여부를 결정한다")
    void observesOriginalBlackoutAfterRollback(Change change) throws Exception {
        raceWithBlackoutFirst(change, true);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("예약이 방을 먼저 잠그면 차단 변경은 예약 판단과 트랜잭션 종료까지 대기한다")
    void blackoutChangeWaitsForReservationTransaction(Change change) throws Exception {
        seedBlackout(change);
        var gate = new LockGate();
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var reservation =
                        executor.submit(
                                () ->
                                        reservationOutcome(
                                                change.initiallyBlocked,
                                                gate::hold,
                                                CLOCK,
                                                gate::recordHolder));
                gate.awaitHeld();
                var mutation =
                        executor.submit(() -> mutate(change, () -> {}, false, gate::recordWaiter));
                gate.awaitBlocked();
                gate.release();
                var result = reservation.get(20, TimeUnit.SECONDS);
                mutation.get(20, TimeUnit.SECONDS);
                assertReservation(result, change.initiallyBlocked);
                assertBlackoutState(change, false);
            } finally {
                gate.release();
            }
        }
    }

    @Test
    @DisplayName("예약은 대기 중 서버 시각이 차단 구간으로 바뀌어도 실제 이용 구간이 겹치지 않으면 허용한다")
    void ignoresBookingClockWhenEvaluatingServiceUsage() throws Exception {
        var startsAt = NOW.plusSeconds(30);
        var bookingTime = new AtomicReference<>(NOW);
        var reservationClock = suppliedClock(bookingTime::get, CLOCK.getZone());
        var gate = new LockGate();
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var mutation =
                        executor.submit(
                                () ->
                                        transaction()
                                                .executeWithoutResult(
                                                        status -> {
                                                            gate.recordHolder();
                                                            creation(lockPort(() -> {}, gate::hold))
                                                                    .create(
                                                                            CreateBlackoutScheduleCommand
                                                                                    .of(
                                                                                            roomId,
                                                                                            morning,
                                                                                            InstantRange
                                                                                                    .of(
                                                                                                            startsAt,
                                                                                                            startsAt
                                                                                                                    .plusSeconds(
                                                                                                                            3600)),
                                                                                            "Maintenance"));
                                                        }));
                gate.awaitHeld();
                var reservation =
                        executor.submit(
                                () ->
                                        reservationOutcome(
                                                false,
                                                () -> {},
                                                reservationClock,
                                                gate::recordWaiter));
                gate.awaitBlocked();
                bookingTime.set(startsAt);
                gate.release();
                mutation.get(20, TimeUnit.SECONDS);
                assertReservation(reservation.get(20, TimeUnit.SECONDS), false);
            } finally {
                gate.release();
            }
        }
    }

    private void raceWithBlackoutFirst(Change change, boolean rollback) throws Exception {
        seedBlackout(change);
        var blocked = rollback ? change.initiallyBlocked : change.blockedAfterCommit;
        var gate = new LockGate();
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var mutation =
                        executor.submit(
                                () -> mutate(change, gate::hold, rollback, gate::recordHolder));
                gate.awaitHeld();
                var reservation =
                        executor.submit(
                                () ->
                                        reservationOutcome(
                                                blocked, () -> {}, CLOCK, gate::recordWaiter));
                gate.awaitBlocked();
                gate.release();
                mutation.get(20, TimeUnit.SECONDS);
                assertReservation(reservation.get(20, TimeUnit.SECONDS), blocked);
                assertBlackoutState(change, rollback);
            } finally {
                gate.release();
            }
        }
    }

    private Optional<CreateReservationResult> reservationOutcome(
            boolean blocked, Runnable afterLock, Clock clock, Runnable beforeCreate) {
        if (blocked) {
            assertThatThrownBy(() -> reserve(afterLock, clock, beforeCreate))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("room usage is blocked by blackout.");
            return Optional.empty();
        }
        return Optional.of(reserve(afterLock, clock, beforeCreate));
    }

    private CreateReservationResult reserve(
            Runnable afterLock, Clock clock, Runnable beforeCreate) {
        return transaction()
                .execute(
                        status -> {
                            beforeCreate.run();
                            return new CreateReservationService(
                                            reservationAdapter,
                                            allocationAdapter,
                                            roomAdapter,
                                            seatAdapter,
                                            lockPort(afterLock, () -> {}),
                                            blackoutAdapter,
                                            UUID::randomUUID,
                                            clock)
                                    .create(
                                            CreateReservationCommand.of(
                                                    roomId, seatId, Set.of(afternoon), DATE));
                        });
    }

    private void mutate(
            Change change, Runnable afterLock, boolean rollback, Runnable beforeChange) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            beforeChange.run();
                            var locks = lockPort(() -> {}, afterLock);
                            var update =
                                    new UpdateBlackoutScheduleService(
                                            blackoutAdapter,
                                            blackoutAdapter,
                                            locks,
                                            blackoutAdapter,
                                            CLOCK,
                                            roomAdapter);
                            switch (change) {
                                case CREATE ->
                                        creation(locks)
                                                .create(
                                                        CreateBlackoutScheduleCommand.of(
                                                                roomId,
                                                                morning,
                                                                activeRange(),
                                                                "Maintenance"));
                                case ACTIVATE_PERIOD ->
                                        update.updateRange(
                                                UpdateBlackoutSchedulePeriodCommand.of(
                                                        blackoutId, activeRange()));
                                case DEACTIVATE_PERIOD ->
                                        update.updateRange(
                                                UpdateBlackoutSchedulePeriodCommand.of(
                                                        blackoutId, futureRange()));
                                case DELETE ->
                                        update.delete(DeleteBlackoutScheduleCommand.of(blackoutId));
                            }
                            entityManager.flush();
                            if (rollback) status.setRollbackOnly();
                        });
    }

    private CreateBlackoutScheduleService creation(ScheduleLockFindPort locks) {
        return new CreateBlackoutScheduleService(
                blackoutAdapter, roomAdapter, locks, blackoutId::getValue, CLOCK);
    }

    private ScheduleLockFindPort lockPort(Runnable afterSharedLock, Runnable afterExclusiveLock) {
        return new ScheduleLockFindPort() {
            @Override
            public Optional<Set<ScheduleId>> findAllByIdsWithSharedLock(Set<ScheduleId> ids) {
                var result = scheduleAdapter.findAllByIdsWithSharedLock(ids);
                afterSharedLock.run();
                return result;
            }

            @Override
            public Optional<Schedule> findByIdWithExclusiveLock(ScheduleId id) {
                var result = scheduleAdapter.findByIdWithExclusiveLock(id);
                // Pause before mutation or flush: a later UPDATE must not mask a missing read lock.
                afterExclusiveLock.run();
                return result;
            }
        };
    }

    private void seedBlackout(Change change) {
        if (change == Change.CREATE) return;
        var range = change.initiallyBlocked ? activeRange() : futureRange();
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager.persist(
                                        BlackoutSchedule.of(
                                                blackoutId,
                                                roomId,
                                                morning,
                                                "Maintenance",
                                                range.startsAt(),
                                                range.endsAt(),
                                                CREATED_AT)));
    }

    private void assertReservation(Optional<CreateReservationResult> result, boolean blocked) {
        assertThat(result.isEmpty()).isEqualTo(blocked);
        result.ifPresent(
                created -> {
                    assertThat(created.scheduleIds()).containsExactly(afternoon);
                    assertThat(created.reservationDate()).isEqualTo(DATE);
                });
        transaction()
                .executeWithoutResult(
                        status -> {
                            assertThat(countRows("reservation")).isEqualTo(blocked ? 0 : 1);
                            assertThat(countRows("reserved_schedule")).isEqualTo(blocked ? 0 : 1);
                        });
    }

    private void assertBlackoutState(Change change, boolean rollback) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            if (change == Change.CREATE && rollback) {
                                assertThat(countRows("blackout_schedule")).isZero();
                                return;
                            }
                            var stored = entityManager.find(BlackoutSchedule.class, blackoutId);
                            assertThat(stored).isNotNull();
                            assertThat(stored.isDeleted())
                                    .isEqualTo(change == Change.DELETE && !rollback);
                            var expectedRange =
                                    rollback
                                            ? (change.initiallyBlocked
                                                    ? activeRange()
                                                    : futureRange())
                                            : (change == Change.DEACTIVATE_PERIOD
                                                    ? futureRange()
                                                    : activeRange());
                            assertThat(stored.getStartsAt()).isEqualTo(expectedRange.startsAt());
                            assertThat(stored.getEndsAt()).isEqualTo(expectedRange.endsAt());
                        });
    }

    private long countRows(String table) {
        return ((Number)
                        entityManager
                                .createNativeQuery(
                                        "SELECT count(*) FROM " + table + " WHERE room_id = :id")
                                .setParameter("id", roomId.getValue())
                                .getSingleResult())
                .longValue();
    }

    private int backendPid() {
        return ((Number)
                        entityManager
                                .createNativeQuery("SELECT pg_backend_pid()")
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
                                                            "SELECT :blocker = any(pg_blocking_pids(:blocked))",
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

    private void persistSchedule(ScheduleId id, int hour) {
        entityManager.persist(
                Schedule.of(
                        id,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(hour, 0),
                        LocalTime.of(hour + 1, 0),
                        CREATED_AT));
    }

    private InstantRange activeRange() {
        return InstantRange.of(
                DATE.atTime(14, 0).atZone(CLOCK.getZone()).toInstant(),
                DATE.atTime(15, 0).atZone(CLOCK.getZone()).toInstant());
    }

    private InstantRange futureRange() {
        return InstantRange.of(
                DATE.plusWeeks(1).atTime(14, 0).atZone(CLOCK.getZone()).toInstant(),
                DATE.plusWeeks(1).atTime(15, 0).atZone(CLOCK.getZone()).toInstant());
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private Clock suppliedClock(Supplier<Instant> time, ZoneId zone) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zone;
            }

            @Override
            public Clock withZone(ZoneId newZone) {
                return newZone.equals(zone) ? this : suppliedClock(time, newZone);
            }

            @Override
            public Instant instant() {
                return time.get();
            }
        };
    }

    private class LockGate {
        private final CountDownLatch held = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final CountDownLatch waiterStarted = new CountDownLatch(1);
        private final AtomicInteger holderPid = new AtomicInteger();
        private final AtomicInteger waiterPid = new AtomicInteger();

        void recordHolder() {
            holderPid.set(backendPid());
        }

        void recordWaiter() {
            waiterPid.set(backendPid());
            waiterStarted.countDown();
        }

        void hold() {
            held.countDown();
            await(released);
        }

        void awaitHeld() {
            await(held);
        }

        void awaitBlocked() {
            await(waiterStarted);
            awaitBlockedBy(waiterPid.get(), holderPid.get());
        }

        void release() {
            released.countDown();
        }
    }

    private enum Change {
        CREATE(false, true),
        ACTIVATE_PERIOD(false, true),
        DEACTIVATE_PERIOD(true, false),
        DELETE(true, false);
        private final boolean initiallyBlocked;
        private final boolean blockedAfterCommit;

        Change(boolean initiallyBlocked, boolean blockedAfterCommit) {
            this.initiallyBlocked = initiallyBlocked;
            this.blockedAfterCommit = blockedAfterCommit;
        }
    }
}
