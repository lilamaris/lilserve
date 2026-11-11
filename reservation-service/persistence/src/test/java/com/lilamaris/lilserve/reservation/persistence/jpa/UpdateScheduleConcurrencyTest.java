package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.service.UpdateScheduleService;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.ReservedSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ReservedScheduleId;
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
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("스케줄 교체와 삭제의 동시성과 트랜잭션")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({ScheduleJpaAdapter.class, UpdateScheduleConcurrencyTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UpdateScheduleConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final String TIME_ZONE = "Asia/Seoul";
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END = LocalTime.of(11, 0);

    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;
    private final ScheduleJpaAdapter adapter;
    private final UpdateScheduleService service;
    private final UpdateScheduleService failingSaveService;
    private final UpdateScheduleService failingIdService;

    private final RoomId roomId = RoomId.of(UUID.randomUUID());
    private final SeatId seatId = SeatId.of(UUID.randomUUID());
    private final ScheduleId originalId = ScheduleId.of(UUID.randomUUID());
    private final ReservationId reservationId = ReservationId.of(UUID.randomUUID());
    private final ReservedScheduleId allocationId = ReservedScheduleId.of(UUID.randomUUID());

    @Autowired
    UpdateScheduleConcurrencyTest(
            EntityManager entityManager,
            PlatformTransactionManager transactionManager,
            ScheduleJpaAdapter adapter,
            @Qualifier("replacementService") UpdateScheduleService service,
            @Qualifier("failingSaveService") UpdateScheduleService failingSaveService,
            @Qualifier("failingIdService") UpdateScheduleService failingIdService) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.adapter = adapter;
        this.service = service;
        this.failingSaveService = failingSaveService;
        this.failingIdService = failingIdService;
    }

    @BeforeEach
    void commitsOriginalAndExistingReservation() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
                            entityManager.persist(
                                    Schedule.of(
                                            originalId,
                                            roomId,
                                            DayOfWeek.MONDAY,
                                            START,
                                            END,
                                            CREATED_AT));
                            entityManager.persist(
                                    Reservation.of(
                                            reservationId,
                                            roomId,
                                            seatId,
                                            DATE,
                                            TIME_ZONE,
                                            CREATED_AT));
                            entityManager.persist(
                                    ReservedSchedule.of(
                                            allocationId,
                                            reservationId,
                                            roomId,
                                            seatId,
                                            originalId,
                                            START,
                                            END,
                                            DATE,
                                            TIME_ZONE,
                                            CREATED_AT));
                        });
    }

    @AfterEach
    void deletesThisTestsRows() {
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
                                                "delete from " + table + " where room_id = :id")
                                        .setParameter("id", roomId.getValue())
                                        .executeUpdate();
                            }
                            entityManager
                                    .createNativeQuery("delete from room where id = :id")
                                    .setParameter("id", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @ParameterizedTest
    @EnumSource(Replacement.class)
    @DisplayName("대체 요일과 시간이 겹치지 않아도 동일 원본의 교체는 한 요청만 성공한다")
    void commitsOnlyOneReplacementAndPreservesOriginal(Replacement first) throws Exception {
        var replacement = race(first, false);

        assertReplacement(replacement, first, NOW);
        assertOriginalAndReservation(NOW);
    }

    @Test
    @DisplayName("선행 교체가 롤백되면 대기한 교체가 활성 원본을 조회하여 하나의 대체만 확정한다")
    void replacesOriginalAfterPrecedingReplacementRollsBack() throws Exception {
        var replacement = race(Replacement.TUESDAY, true);

        assertReplacement(replacement, Replacement.WEDNESDAY, LATER);
        assertOriginalAndReservation(LATER);
    }

    @Test
    @DisplayName("삭제가 먼저 확정되면 대기한 교체를 거부하고 대체 없이 최초 폐기 시각을 보존한다")
    void rejectsReplacementAfterDeletionCommits() throws Exception {
        var replacement = raceWithDeletion(Change.DELETE, Change.REPLACE);

        assertThat(replacement).isEmpty();
        assertScheduleIds(originalId);
        assertOriginalAndReservation(NOW);
    }

    @Test
    @DisplayName("교체가 먼저 확정되면 대기한 삭제를 거부하고 교체 결과와 최초 폐기 시각을 보존한다")
    void rejectsDeletionAfterReplacementCommits() throws Exception {
        var replacement = raceWithDeletion(Change.REPLACE, Change.DELETE).orElseThrow();

        assertReplacement(replacement, Replacement.TUESDAY, NOW);
        assertOriginalAndReservation(NOW);
    }

    @Test
    @DisplayName("중복 삭제의 후속 요청을 거부하고 최초 폐기 시각과 기존 예약 참조를 보존한다")
    void rejectsDuplicateDeletionAndPreservesFirstTimestamp() throws Exception {
        var replacement = raceWithDeletion(Change.DELETE, Change.DELETE);

        assertThat(replacement).isEmpty();
        assertScheduleIds(originalId);
        assertOriginalAndReservation(NOW);
    }

    @Test
    @DisplayName("삭제 저장과 플러시 후 예외가 발생하면 폐기 상태를 롤백하고 기존 예약 참조를 보존한다")
    void rollsBackFlushedRetirementWhenDeletionFails() {
        assertThatThrownBy(() -> failingSaveService.delete(originalId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");

        assertScheduleIds(originalId);
        assertOriginalAndReservation(null);
    }

    @Test
    @DisplayName("원본과 같은 시간으로 교체해도 원본 폐기가 먼저 플러시되어 대체를 저장한다")
    void flushesRetirementBeforeSavingOverlappingReplacement() {
        var replacement = service.update(command(DayOfWeek.MONDAY, START, END));

        assertStoredReplacement(replacement);
        assertThat(replacement.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(replacement.startAt()).isEqualTo(START);
        assertThat(replacement.endAt()).isEqualTo(END);
        assertOriginalAndReservation(NOW);
    }

    @Test
    @DisplayName("대체 저장이 기존 활성 슬롯과 충돌하면 원본 폐기까지 롤백하고 대체를 남기지 않는다")
    void rollsBackRetirementWhenReplacementViolatesDatabaseConstraint() {
        var conflict = ScheduleId.of(UUID.randomUUID());
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager.persist(
                                        Schedule.of(
                                                conflict,
                                                roomId,
                                                DayOfWeek.TUESDAY,
                                                START,
                                                END,
                                                CREATED_AT)));

        assertThatThrownBy(() -> service.update(command(DayOfWeek.TUESDAY, START, END)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertScheduleIds(originalId, conflict);
        assertOriginalAndReservation(null);
    }

    @Test
    @DisplayName("대체 저장과 플러시 후 예외가 발생하면 원본 폐기와 대체 생성을 모두 롤백한다")
    void rollsBackRetirementAndFlushedReplacementOnFailure() {
        assertThatThrownBy(() -> failingSaveService.update(command(Replacement.TUESDAY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");

        assertScheduleIds(originalId);
        assertOriginalAndReservation(null);
    }

    @Test
    @DisplayName("원본 엔티티 변경 후 식별자 생성이 실패해도 원본 상태를 롤백하고 대체를 남기지 않는다")
    void rollsBackManagedRetirementWhenIdGenerationFails() {
        assertThatThrownBy(() -> failingIdService.update(command(Replacement.TUESDAY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Identifier generation failed");

        assertScheduleIds(originalId);
        assertOriginalAndReservation(null);
    }

    private Optional<UpdateScheduleResult> raceWithDeletion(Change first, Change second)
            throws Exception {
        var firstLocked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var firstPid = new AtomicInteger();
        var secondPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstRequest =
                    executor.submit(
                            () ->
                                    change(
                                            first,
                                            NOW,
                                            () -> firstPid.set(backendPid()),
                                            () -> {
                                                firstLocked.countDown();
                                                await(releaseFirst);
                                            }));
            await(firstLocked);
            var secondRequest =
                    executor.submit(
                            () ->
                                    rejectWaitingChange(
                                            second,
                                            () -> {
                                                secondPid.set(backendPid());
                                                secondStarted.countDown();
                                            }));
            await(secondStarted);
            awaitBlockedBy(secondPid.get(), firstPid.get());
            releaseFirst.countDown();
            var result = firstRequest.get(20, TimeUnit.SECONDS);
            secondRequest.get(20, TimeUnit.SECONDS);
            return result;
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void rejectWaitingChange(Change change, Runnable beforeLock) {
        assertThatThrownBy(() -> change(change, LATER, beforeLock, () -> {}))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("schedule does not exists.");
    }

    private Optional<UpdateScheduleResult> change(
            Change change, Instant now, Runnable beforeLock, Runnable afterLock) {
        var changeService =
                new UpdateScheduleService(
                        pausingLockPort(afterLock),
                        adapter,
                        adapter,
                        UUID::randomUUID,
                        Clock.fixed(now, ZoneOffset.UTC));
        return transaction()
                .execute(
                        status -> {
                            beforeLock.run();
                            return switch (change) {
                                case REPLACE ->
                                        Optional.of(
                                                changeService.update(command(Replacement.TUESDAY)));
                                case DELETE -> {
                                    changeService.delete(originalId);
                                    yield Optional.empty();
                                }
                            };
                        });
    }

    private ScheduleLockFindPort pausingLockPort(Runnable afterLock) {
        return new ScheduleLockFindPort() {
            @Override
            public Optional<Set<ScheduleId>> findAllByIdsWithSharedLock(Set<ScheduleId> ids) {
                return adapter.findAllByIdsWithSharedLock(ids);
            }

            @Override
            public Optional<Schedule> findByIdWithExclusiveLock(ScheduleId id) {
                var locked = adapter.findByIdWithExclusiveLock(id);
                // Pause before any mutation or flush, so an UPDATE lock cannot mask a missing
                // exclusive read lock in either the replacement or deletion path.
                afterLock.run();
                return locked;
            }
        };
    }

    private UpdateScheduleResult race(Replacement first, boolean rollback) throws Exception {
        var firstLocked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var firstPid = new AtomicInteger();
        var secondPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        var second = first == Replacement.TUESDAY ? Replacement.WEDNESDAY : Replacement.TUESDAY;
        try {
            var firstRequest =
                    executor.submit(
                            () ->
                                    replace(
                                            first,
                                            NOW,
                                            rollback,
                                            () -> firstPid.set(backendPid()),
                                            () -> {
                                                firstLocked.countDown();
                                                await(releaseFirst);
                                            }));
            await(firstLocked);
            var secondRequest =
                    executor.submit(
                            () ->
                                    runWaitingReplacement(
                                            second,
                                            rollback,
                                            () -> {
                                                secondPid.set(backendPid());
                                                secondStarted.countDown();
                                            }));
            await(secondStarted);
            awaitBlockedBy(secondPid.get(), firstPid.get());
            releaseFirst.countDown();
            var firstResult = firstRequest.get(20, TimeUnit.SECONDS);
            var secondResult = secondRequest.get(20, TimeUnit.SECONDS);
            return rollback ? secondResult : firstResult;
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private UpdateScheduleResult runWaitingReplacement(
            Replacement replacement, boolean precedingRollback, Runnable beforeLock) {
        if (precedingRollback) return replace(replacement, LATER, false, beforeLock, () -> {});
        assertThatThrownBy(() -> replace(replacement, LATER, false, beforeLock, () -> {}))
                .isInstanceOf(NoSuchElementException.class);
        return null;
    }

    private UpdateScheduleResult replace(
            Replacement replacement,
            Instant now,
            boolean rollback,
            Runnable beforeLock,
            Runnable afterLock) {
        // Identifier generation follows the exclusive read and precedes the bulk retirement flush.
        // Pausing here proves that an UPDATE lock cannot hide a missing exclusive read lock.
        var replacementService =
                new UpdateScheduleService(
                        adapter,
                        adapter,
                        adapter,
                        () -> {
                            afterLock.run();
                            return UUID.randomUUID();
                        },
                        Clock.fixed(now, ZoneOffset.UTC));
        return transaction()
                .execute(
                        status -> {
                            beforeLock.run();
                            var result = replacementService.update(command(replacement));
                            if (rollback) status.setRollbackOnly();
                            return result;
                        });
    }

    private UpdateScheduleCommand command(Replacement replacement) {
        return switch (replacement) {
            case TUESDAY -> command(DayOfWeek.TUESDAY, START, END);
            case WEDNESDAY ->
                    command(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        };
    }

    private UpdateScheduleCommand command(DayOfWeek day, LocalTime start, LocalTime end) {
        return UpdateScheduleCommand.of(originalId, LocalTimeRange.of(start, end), day);
    }

    private void assertReplacement(
            UpdateScheduleResult result, Replacement replacement, Instant now) {
        var expected = command(replacement);
        assertThat(result.dayOfWeek()).isEqualTo(expected.dayOfWeek());
        assertThat(result.startAt()).isEqualTo(expected.range().startAt());
        assertThat(result.endAt()).isEqualTo(expected.range().endAt());
        assertThat(result.createdAt()).isEqualTo(now);
        assertStoredReplacement(result);
    }

    private void assertStoredReplacement(UpdateScheduleResult result) {
        assertThat(result.scheduleId()).isNotEqualTo(originalId);
        assertThat(result.roomId()).isEqualTo(roomId);
        assertScheduleIds(originalId, result.scheduleId());
        transaction()
                .executeWithoutResult(
                        status -> {
                            var saved = entityManager.find(Schedule.class, result.scheduleId());
                            assertThat(UpdateScheduleResult.from(saved)).isEqualTo(result);
                            assertThat(saved.isDeleted()).isFalse();
                        });
    }

    private void assertScheduleIds(ScheduleId... ids) {
        transaction()
                .executeWithoutResult(
                        status ->
                                assertThat(
                                                entityManager
                                                        .createQuery(
                                                                "SELECT s.id FROM Schedule s WHERE s.roomId = :roomId",
                                                                ScheduleId.class)
                                                        .setParameter("roomId", roomId)
                                                        .getResultList())
                                        .containsExactlyInAnyOrder(ids));
    }

    private void assertOriginalAndReservation(Instant retiredAt) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            var original = entityManager.find(Schedule.class, originalId);
                            assertThat(original.getRoomId()).isEqualTo(roomId);
                            assertThat(original.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                            assertThat(original.getStartAt()).isEqualTo(START);
                            assertThat(original.getEndAt()).isEqualTo(END);
                            assertThat(original.getCreatedAt()).isEqualTo(CREATED_AT);
                            assertThat(original.getDeletedAt()).isEqualTo(retiredAt);
                            var reservation = entityManager.find(Reservation.class, reservationId);
                            assertThat(reservation.getDeletedAt()).isNull();
                            var allocation =
                                    entityManager.find(ReservedSchedule.class, allocationId);
                            assertThat(allocation.getReservationId()).isEqualTo(reservationId);
                            assertThat(allocation.getScheduleId()).isEqualTo(originalId);
                            assertThat(allocation.getStartAt()).isEqualTo(START);
                            assertThat(allocation.getEndAt()).isEqualTo(END);
                            assertThat(allocation.getStartsAt())
                                    .isEqualTo(Instant.parse("2026-11-16T01:00:00Z"));
                            assertThat(allocation.getEndsAt())
                                    .isEqualTo(Instant.parse("2026-11-16T02:00:00Z"));
                            assertThat(allocation.getDeletedAt()).isNull();
                        });
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

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private enum Change {
        REPLACE,
        DELETE
    }

    private enum Replacement {
        TUESDAY,
        WEDNESDAY
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        UpdateScheduleService replacementService(ScheduleJpaAdapter adapter) {
            return new UpdateScheduleService(
                    adapter, adapter, adapter, UUID::randomUUID, Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        UpdateScheduleService failingSaveService(
                ScheduleJpaAdapter adapter, EntityManager entityManager) {
            return new UpdateScheduleService(
                    adapter,
                    new ScheduleSavePort() {
                        @Override
                        public void save(Schedule schedule) {
                            adapter.save(schedule);
                            entityManager.flush();
                            throw new IllegalStateException("Storage failed after flush");
                        }

                        @Override
                        public void saveAll(Collection<Schedule> schedules) {
                            adapter.saveAll(schedules);
                        }
                    },
                    adapter,
                    UUID::randomUUID,
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        UpdateScheduleService failingIdService(ScheduleJpaAdapter adapter) {
            return new UpdateScheduleService(
                    adapter,
                    adapter,
                    adapter,
                    () -> {
                        throw new IllegalStateException("Identifier generation failed");
                    },
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
