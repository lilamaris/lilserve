package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.service.CreateReservationService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Verifies header and allocation transactions against PostgreSQL using real persistence and
 * resource lock adapters. Resource-state races are covered by CreateReservationConcurrencyTest.
 */
@DisplayName("예약 슬롯 저장의 서비스 트랜잭션 경계")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    CreateReservationService.class,
    ReservedScheduleJpaAdapter.class,
    ReservationJpaAdapter.class,
    RoomJpaAdapter.class,
    SeatJpaAdapter.class,
    ScheduleJpaAdapter.class,
    ReservedScheduleTransactionTest.Configuration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservedScheduleTransactionTest {
    private static final Instant NOW = Instant.parse("2026-11-05T00:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 9);

    @Autowired private CreateReservationUseCase service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private final RoomId roomId = RoomId.of(UUID.randomUUID());
    private final SeatId seatId = SeatId.of(UUID.randomUUID());
    private ScheduleId morning;
    private ScheduleId afternoon;

    @BeforeEach
    void commitsAvailableSeatAndSchedules() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "test room", NOW));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", NOW));
                            morning = persistSchedule(10, 12);
                            afternoon = persistSchedule(14, 16);
                        });
    }

    @AfterEach
    void deletesOnlyThisTestsCommittedRows() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            for (var table :
                                    new String[] {
                                        "reserved_schedule", "reservation", "schedule", "seat"
                                    }) {
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

    @Test
    @DisplayName("일부 슬롯이 비활성이면 서비스가 예외를 던지고 헤더와 삽입된 점유를 모두 롤백한다")
    void rollsBackPartialInsertionThroughService() {
        transaction()
                .executeWithoutResult(
                        status -> entityManager.find(Schedule.class, afternoon).delete(NOW));
        var command = command(Set.of(morning, afternoon));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to reserve schedules.");

        assertRowCounts(0, 0);
    }

    @Test
    @DisplayName("충돌한 새 예약은 전체 롤백하고 이미 확정된 예약과 점유는 보존한다")
    void preservesCommittedReservationWhenNewReservationConflicts() {
        var result = service.create(command(Set.of(morning)));
        assertThat(result.scheduleIds()).containsExactly(morning);
        var originalId =
                transaction()
                        .execute(
                                status ->
                                        entityManager
                                                .createNativeQuery(
                                                        "select id from reserved_schedule where room_id = :roomId",
                                                        UUID.class)
                                                .setParameter("roomId", roomId.getValue())
                                                .getSingleResult());

        assertThatThrownBy(() -> service.create(command(Set.of(morning, afternoon))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to reserve schedules.");

        assertRowCounts(1, 1);
        var remainingId =
                transaction()
                        .execute(
                                status ->
                                        entityManager
                                                .createNativeQuery(
                                                        "select id from reserved_schedule where room_id = :roomId",
                                                        UUID.class)
                                                .setParameter("roomId", roomId.getValue())
                                                .getSingleResult());
        assertThat(remainingId).isEqualTo(originalId);
    }

    @Test
    @DisplayName("동일 좌석과 슬롯의 동시 예약은 한 요청만 확정하고 실패한 요청의 헤더는 남지 않는다")
    void commitsOnlyOneConcurrentReservation() throws Exception {
        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> reserveAfterBarrier(start));
            var second = executor.submit(() -> reserveAfterBarrier(start));

            assertThat(
                            new Boolean[] {
                                first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)
                            })
                    .containsExactlyInAnyOrder(true, false);
        }

        assertRowCounts(1, 1);
    }

    private boolean reserveAfterBarrier(CyclicBarrier start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            service.create(command(Set.of(morning)));
            return true;
        } catch (IllegalStateException exception) {
            assertThat(exception).hasMessage("failed to reserve schedules.");
            return false;
        }
    }

    private void assertRowCounts(long reservations, long allocations) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            assertThat(countRows("reservation")).isEqualTo(reservations);
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

    private ScheduleId persistSchedule(int start, int end) {
        var schedule =
                Schedule.of(
                        ScheduleId.of(UUID.randomUUID()),
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(start, 0),
                        LocalTime.of(end, 0),
                        NOW);
        entityManager.persist(schedule);
        return schedule.getId();
    }

    private CreateReservationCommand command(Set<ScheduleId> schedules) {
        return CreateReservationCommand.of(roomId, seatId, schedules, DATE);
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    /** Supplies a fixed clock and random identifiers. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));
        }

        @Bean
        IdGenerator<UUID> uuidGenerator() {
            return UUID::randomUUID;
        }
    }
}
