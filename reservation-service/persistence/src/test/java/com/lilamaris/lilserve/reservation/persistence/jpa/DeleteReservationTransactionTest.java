package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.service.DeleteReservationService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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

@DisplayName("예약 취소의 서비스 트랜잭션 경계")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    DeleteReservationService.class,
    ReservationJpaAdapter.class,
    ReservedScheduleJpaAdapter.class,
    DeleteReservationTransactionTest.Configuration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeleteReservationTransactionTest {
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);
    private static final LocalDate DATE = LocalDate.of(2026, 11, 9);
    private static final String TIME_ZONE = "Asia/Seoul";

    @Autowired private DeleteReservationUseCase service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private final RoomId roomId = RoomId.of(UUID.randomUUID());
    private final SeatId seatId = SeatId.of(UUID.randomUUID());

    @BeforeEach
    void commitsRoomAndSeat() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
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

    @Test
    @DisplayName("예약 헤더와 모든 점유의 취소를 커밋하고 같은 취소 시각을 반환한다")
    void commitsHeaderAndAllAllocationCancellations() {
        var reservationId = commitReservation(10, CREATED_AT);

        var result = service.delete(DeleteReservationCommand.of(reservationId));

        assertThat(result.reservationId()).isEqualTo(reservationId);
        assertThat(result.deletedAt()).isEqualTo(NOW);
        var persisted = snapshot(reservationId);
        assertThat(persisted.headerDeletedAt()).isEqualTo(result.deletedAt());
        assertThat(persisted.allocationDeletedAt()).containsExactly(NOW, NOW);
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 9})
    @DisplayName("첫 점유가 이미 시작했거나 시작 시각과 같으면 취소하지 않는다")
    void preservesReservationAtOrAfterEarliestStart(int startHour) {
        var reservationId = commitReservation(startHour, CREATED_AT);

        assertThatThrownBy(() -> service.delete(DeleteReservationCommand.of(reservationId)))
                .isInstanceOf(NoSuchElementException.class);

        assertActive(reservationId);
    }

    @Test
    @DisplayName("이미 취소된 예약의 재취소를 거부하고 저장된 취소 상태를 유지한다")
    void rejectsAlreadyCancelledReservation() {
        var reservationId = commitReservation(10, CREATED_AT);
        service.delete(DeleteReservationCommand.of(reservationId));
        var cancelled = snapshot(reservationId);

        assertThatThrownBy(() -> service.delete(DeleteReservationCommand.of(reservationId)))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(snapshot(reservationId)).isEqualTo(cancelled);
    }

    @Test
    @DisplayName("점유 취소가 DB 제약으로 실패하면 헤더와 모든 점유의 취소를 롤백한다")
    void rollsBackHeaderAndAllocationsWhenReleaseViolatesConstraint() {
        // A valid stored allocation created after NOW makes cancellation violate deleted_at >=
        // created_at.
        var reservationId = commitReservation(10, NOW.plusSeconds(60));

        assertThatThrownBy(() -> service.delete(DeleteReservationCommand.of(reservationId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertActive(reservationId);
    }

    private void assertActive(ReservationId reservationId) {
        var persisted = snapshot(reservationId);
        assertThat(persisted.headerDeletedAt()).isNull();
        assertThat(persisted.allocationDeletedAt()).hasSize(2).containsOnlyNulls();
    }

    private ReservationId commitReservation(int startHour, Instant secondAllocationCreatedAt) {
        return transaction()
                .execute(
                        status -> {
                            var reservation =
                                    Reservation.of(
                                            ReservationId.of(UUID.randomUUID()),
                                            roomId,
                                            seatId,
                                            DATE,
                                            TIME_ZONE,
                                            CREATED_AT);
                            entityManager.persist(reservation);
                            persistAllocation(reservation, startHour, CREATED_AT);
                            persistAllocation(reservation, 14, secondAllocationCreatedAt);
                            return reservation.getId();
                        });
    }

    private void persistAllocation(Reservation reservation, int startHour, Instant createdAt) {
        var schedule =
                Schedule.of(
                        ScheduleId.of(UUID.randomUUID()),
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(startHour, 0),
                        LocalTime.of(startHour + 1, 0),
                        CREATED_AT);
        entityManager.persist(schedule);
        entityManager.persist(
                ReservedSchedule.of(
                        ReservedScheduleId.of(UUID.randomUUID()),
                        reservation.getId(),
                        roomId,
                        seatId,
                        schedule.getId(),
                        schedule.getStartAt(),
                        schedule.getEndAt(),
                        DATE,
                        TIME_ZONE,
                        createdAt));
    }

    private CancellationSnapshot snapshot(ReservationId reservationId) {
        return transaction()
                .execute(
                        status -> {
                            var header = entityManager.find(Reservation.class, reservationId);
                            var allocations =
                                    entityManager
                                            .createQuery(
                                                    """
                    SELECT rs FROM ReservedSchedule rs
                    WHERE rs.reservationId = :reservationId
                    ORDER BY rs.startsAt
                    """,
                                                    ReservedSchedule.class)
                                            .setParameter("reservationId", reservationId)
                                            .getResultList();
                            return new CancellationSnapshot(
                                    header.getDeletedAt(),
                                    allocations.stream()
                                            .map(ReservedSchedule::getDeletedAt)
                                            .toList());
                        });
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private record CancellationSnapshot(
            Instant headerDeletedAt, List<Instant> allocationDeletedAt) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneId.of(TIME_ZONE));
        }
    }
}
