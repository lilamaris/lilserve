package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("예약 가능한 좌석 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(AvailableSeatJpaAdapter.class)
class AvailableSeatJpaAdapterTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant CANCELLED = CREATED.plusSeconds(60);
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 16);
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Autowired private AvailableSeatJpaAdapter adapter;
    @Autowired private TestEntityManager entityManager;
    private Room room;

    @BeforeEach
    void setUp() {
        room = persistRoom("ROOM-01");
    }

    @Test
    @DisplayName("네이티브 조회는 해당 방의 활성 좌석을 엔티티로 매핑하고 코드순으로 반환한다")
    void mapsAndOrdersActiveSeatsInRequestedRoom() {
        var selected = persistSchedule(10, 12);
        var second = persistSeat(room, "B-01");
        var first = persistSeat(room, "A-01");
        var deleted = persistSeat(room, "C-01");
        deleted.delete(CANCELLED);
        persistSeat(persistRoom("ROOM-02"), "D-01");
        flush();

        var seats =
                adapter.findAvailableSeat(room.getId(), TARGET_DATE, Set.of(selected.getId()), UTC);

        assertThat(seats).extracting(Seat::getId).containsExactly(first.getId(), second.getId());
        assertThat(seats.getFirst().getRoomId()).isEqualTo(room.getId());
        assertThat(seats.getFirst().getCreatedAt()).isEqualTo(CREATED);
    }

    @ParameterizedTest
    @CsvSource({"UTC, false", "Asia/Seoul, true"})
    @DisplayName("전달한 시간대로 선택 슬롯을 해석하여 기존 점유와 실제 시각이 겹치는 좌석만 제외한다")
    void bindsRequestedTimezoneWhenComparingOccupiedInstants(String timeZone, boolean available) {
        var selected = persistSchedule(10, 12);
        var seat = persistSeat(room, "A-01");
        persistAllocation(seat, selected);
        flush();

        var seats =
                adapter.findAvailableSeat(
                        room.getId(), TARGET_DATE, Set.of(selected.getId()), ZoneId.of(timeZone));

        assertThat(seats)
                .extracting(Seat::getId)
                .containsExactlyElementsOf(
                        available ? java.util.List.of(seat.getId()) : java.util.List.of());
    }

    @Test
    @DisplayName("이전 버전 슬롯에 남은 점유와 시간이 겹치는 좌석은 새 슬롯 조회에서도 제외한다")
    void excludesOverlappingAllocationOfRetiredSchedule() {
        var original = persistSchedule(10, 12);
        var seat = persistSeat(room, "A-01");
        persistAllocation(seat, original);
        original.delete(CANCELLED);
        entityManager.flush();
        var replacement = persistSchedule(11, 13);
        flush();

        assertThat(
                        adapter.findAvailableSeat(
                                room.getId(), TARGET_DATE, Set.of(replacement.getId()), UTC))
                .isEmpty();
    }

    @Test
    @DisplayName("기존 점유의 종료 경계에서 시작하는 슬롯은 좌석을 제외하지 않는다")
    void allowsAdjacentSlot() {
        var occupied = persistSchedule(10, 12);
        var adjacent = persistSchedule(12, 14);
        var seat = persistSeat(room, "A-01");
        persistAllocation(seat, occupied);
        flush();

        assertThat(
                        adapter.findAvailableSeat(
                                room.getId(), TARGET_DATE, Set.of(adjacent.getId()), UTC))
                .extracting(Seat::getId)
                .containsExactly(seat.getId());
    }

    @Test
    @DisplayName("취소된 점유는 좌석을 예약 가능 목록에서 제외하지 않는다")
    void ignoresCancelledAllocation() {
        var selected = persistSchedule(10, 12);
        var seat = persistSeat(room, "A-01");
        var allocation = persistAllocation(seat, selected);
        allocation.delete(CANCELLED);
        flush();

        assertThat(
                        adapter.findAvailableSeat(
                                room.getId(), TARGET_DATE, Set.of(selected.getId()), UTC))
                .extracting(Seat::getId)
                .containsExactly(seat.getId());
    }

    private Room persistRoom(String code) {
        return entityManager.persist(
                Room.of(RoomId.of(UUID.randomUUID()), code, "Hall", "", CREATED));
    }

    private Seat persistSeat(Room owningRoom, String code) {
        return entityManager.persist(
                Seat.of(SeatId.of(UUID.randomUUID()), owningRoom.getId(), code, CREATED));
    }

    private Schedule persistSchedule(int startHour, int endHour) {
        return entityManager.persist(
                Schedule.of(
                        ScheduleId.of(UUID.randomUUID()),
                        room.getId(),
                        TARGET_DATE.getDayOfWeek(),
                        LocalTime.of(startHour, 0),
                        LocalTime.of(endHour, 0),
                        CREATED));
    }

    private ReservedSchedule persistAllocation(Seat seat, Schedule schedule) {
        var reservation =
                entityManager.persist(
                        Reservation.of(
                                ReservationId.of(UUID.randomUUID()),
                                room.getId(),
                                seat.getId(),
                                TARGET_DATE,
                                UTC.getId(),
                                CREATED));
        return entityManager.persist(
                ReservedSchedule.of(
                        ReservedScheduleId.of(UUID.randomUUID()),
                        reservation.getId(),
                        room.getId(),
                        seat.getId(),
                        schedule.getId(),
                        schedule.getStartAt(),
                        schedule.getEndAt(),
                        TARGET_DATE,
                        UTC.getId(),
                        CREATED));
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }
}
