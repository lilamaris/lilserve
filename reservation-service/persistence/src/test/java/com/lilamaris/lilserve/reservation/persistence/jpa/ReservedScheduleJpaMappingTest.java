package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.ReservedSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("예약 슬롯 JPA 매핑")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ReservedScheduleJpaMappingTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, Month.NOVEMBER, 9);
    private static final String TIME_ZONE = "Asia/Seoul";
    private static final LocalTime MORNING_START = LocalTime.of(10, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);

    @Autowired private TestEntityManager entityManager;

    private RoomId roomId;
    private SeatId seatId;

    @BeforeEach
    void persistsRoomAndSeat() {
        roomId = RoomId.of(UUID.randomUUID());
        seatId = SeatId.of(UUID.randomUUID());

        entityManager.persist(Room.of(roomId, "ROOM-01", "main hall", "capacity 100", CREATED_AT));
        entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
    }

    private ScheduleId persistSlot(LocalTime startAt, LocalTime endAt) {
        var scheduleId = ScheduleId.of(UUID.randomUUID());
        entityManager.persist(
                Schedule.of(
                        scheduleId.getValue(),
                        roomId.getValue(),
                        DayOfWeek.MONDAY,
                        startAt,
                        endAt,
                        CREATED_AT));
        return scheduleId;
    }

    private ReservationId persistReservation() {
        var reservationId = ReservationId.of(UUID.randomUUID());
        entityManager.persist(
                Reservation.of(
                        reservationId.getValue(),
                        roomId.getValue(),
                        seatId.getValue(),
                        SERVICE_DATE,
                        TIME_ZONE,
                        CREATED_AT));
        return reservationId;
    }

    private ReservedSchedule persistAllocation(
            ReservationId reservationId,
            ScheduleId scheduleId,
            LocalTime startAt,
            LocalTime endAt) {
        var allocation =
                ReservedSchedule.of(
                        UUID.randomUUID(),
                        reservationId.getValue(),
                        roomId.getValue(),
                        seatId.getValue(),
                        scheduleId.getValue(),
                        startAt,
                        endAt,
                        SERVICE_DATE,
                        TIME_ZONE,
                        CREATED_AT);
        entityManager.persist(allocation);
        return allocation;
    }

    @Test
    @DisplayName("데이터베이스가 만든 절대 시각 스냅샷을 읽는다")
    void readsGeneratedInstantsFromDatabase() {
        var scheduleId = persistSlot(MORNING_START, MORNING_END);
        var reservationId = persistReservation();
        var allocation = persistAllocation(reservationId, scheduleId, MORNING_START, MORNING_END);
        entityManager.flush();
        entityManager.clear();

        var loaded = entityManager.find(ReservedSchedule.class, allocation.getId());

        assertThat(loaded.getStartsAt()).isEqualTo(Instant.parse("2026-11-09T01:00:00Z"));
        assertThat(loaded.getEndsAt()).isEqualTo(Instant.parse("2026-11-09T03:00:00Z"));
        assertThat(loaded.startInstant()).isEqualTo(loaded.getStartsAt());
        assertThat(loaded.endInstant()).isEqualTo(loaded.getEndsAt());
    }

    @Test
    @DisplayName("서비스 날짜의 요일은 저장된 ISO 요일로 읽힌다")
    void readsIsoDayOfWeekFromDatabase() {
        var scheduleId = persistSlot(MORNING_START, MORNING_END);
        var reservationId = persistReservation();
        var allocation = persistAllocation(reservationId, scheduleId, MORNING_START, MORNING_END);
        entityManager.flush();
        entityManager.clear();

        var loaded = entityManager.find(ReservedSchedule.class, allocation.getId());
        var loadedSchedule = entityManager.find(Schedule.class, scheduleId);

        assertThat(loaded.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(loadedSchedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("같은 좌석의 같은 시간을 두 슬롯이 겹치게 예약할 수 없다")
    void rejectsOverlapCreatedFromReplacedSlot() {
        var firstSlot = persistSlot(MORNING_START, MORNING_END);
        var reservationId = persistReservation();
        persistAllocation(reservationId, firstSlot, MORNING_START, MORNING_END);
        entityManager.flush();

        var retired = entityManager.find(Schedule.class, firstSlot);
        retired.delete(Instant.parse("2026-11-05T00:00:00Z"));
        entityManager.flush();

        var replacementSlot = persistSlot(LocalTime.of(11, 0), LocalTime.of(13, 0));
        entityManager.flush();

        assertThatThrownBy(
                        () -> {
                            persistAllocation(
                                    reservationId,
                                    replacementSlot,
                                    LocalTime.of(11, 0),
                                    LocalTime.of(13, 0));
                            entityManager.flush();
                        })
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ex_reserved_seat_no_overlap");
    }

    @Test
    @DisplayName("같은 좌석의 인접한 시간은 예약할 수 있다")
    void acceptsAdjacentAllocationOnSameSeat() {
        var reservationId = persistReservation();
        var morningSlot = persistSlot(MORNING_START, MORNING_END);
        var afternoonSlot = persistSlot(LocalTime.of(12, 0), LocalTime.of(13, 0));

        var morning = persistAllocation(reservationId, morningSlot, MORNING_START, MORNING_END);
        var afternoon =
                persistAllocation(
                        reservationId, afternoonSlot, LocalTime.of(12, 0), LocalTime.of(13, 0));
        entityManager.flush();
        entityManager.clear();

        var loadedMorning = entityManager.find(ReservedSchedule.class, morning.getId());
        var loadedAfternoon = entityManager.find(ReservedSchedule.class, afternoon.getId());

        assertThat(loadedMorning.getEndsAt()).isEqualTo(loadedAfternoon.getStartsAt());
    }
}
