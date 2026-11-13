package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListAvailableSeatQuery;
import com.lilamaris.lilserve.reservation.application.service.CreateReservationService;
import com.lilamaris.lilserve.reservation.application.service.ListAvailableSeatService;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
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

@DisplayName("방 이용 구간 기준 점검 차단 정책")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    RoomJpaAdapter.class,
    SeatJpaAdapter.class,
    ScheduleJpaAdapter.class,
    ReservationJpaAdapter.class,
    ReservedScheduleJpaAdapter.class,
    BlackoutScheduleJpaAdapter.class,
    AvailableSeatJpaAdapter.class
})
class BlackoutUsagePolicyTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant BOOKED = Instant.parse("2026-11-13T01:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    @Autowired private TestEntityManager em;
    @Autowired private RoomJpaAdapter rooms;
    @Autowired private SeatJpaAdapter seats;
    @Autowired private ScheduleJpaAdapter schedules;
    @Autowired private ReservationJpaAdapter reservations;
    @Autowired private ReservedScheduleJpaAdapter allocations;
    @Autowired private BlackoutScheduleJpaAdapter blackouts;
    @Autowired private AvailableSeatJpaAdapter availableSeats;
    private Room room;
    private Seat seat;
    private Schedule selected;

    @BeforeEach
    void setUp() {
        room = em.persist(Room.of(RoomId.of(UUID.randomUUID()), "ROOM-01", "Hall", "", CREATED));
        seat = em.persist(Seat.of(SeatId.of(UUID.randomUUID()), room.getId(), "A-01", CREATED));
        selected = slot(room, 10, 12);
    }

    @Test
    @DisplayName("미래 이용 구간이 점검과 겹치면 현재 점검 중이 아니어도 예약을 거절하고 빈 좌석 목록을 반환한다")
    void rejectsFutureUsageDuringMaintenance() {
        blackout(room, selected, at(10), at(12));
        flush();
        var clock = Clock.fixed(BOOKED, ZONE);

        assertThat(list(clock, DATE, Set.of(selected.getId())).availableSeatIds()).isEmpty();
        assertThatThrownBy(() -> reserve(clock, DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("room usage is blocked by blackout.");
        assertThat(
                        em.getEntityManager()
                                .createQuery("SELECT count(r) FROM Reservation r", Long.class)
                                .getSingleResult())
                .isZero();
    }

    @Test
    @DisplayName("현재 점검 중이어도 점검과 겹치지 않는 다음 주 이용은 조회와 예약을 허용한다")
    void allowsFutureUsageWhileRoomIsCurrentlyUnderMaintenance() {
        blackout(room, selected, at(10), at(12));
        flush();
        var clock = Clock.fixed(at(11), ZONE);
        var nextWeek = DATE.plusWeeks(1);

        assertThat(list(clock, nextWeek, Set.of(selected.getId())).availableSeatIds())
                .containsExactly(seat.getId());
        assertThat(reserve(clock, nextWeek).reservationDate()).isEqualTo(nextWeek);
    }

    @ParameterizedTest
    @CsvSource({"9,11", "11,13", "9,13", "10,12"})
    @DisplayName("방의 점검 구간이 이용 구간과 일부라도 겹치면 차단한다")
    void blocksAnyOverlap(int start, int end) {
        blackout(room, selected, at(start), at(end));
        flush();
        assertThat(
                        blackouts.existsOverlappingUsage(
                                room.getId(), Set.of(selected.getId()), DATE, ZONE))
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({"8,10", "12,14"})
    @DisplayName("점검과 이용 구간의 시작 또는 종료 경계만 맞닿으면 허용한다")
    void allowsAdjacentUsage(int start, int end) {
        blackout(room, selected, at(start), at(end));
        flush();
        assertThat(
                        blackouts.existsOverlappingUsage(
                                room.getId(), Set.of(selected.getId()), DATE, ZONE))
                .isFalse();
    }

    @Test
    @DisplayName("여러 선택 슬롯 사이 공백의 점검은 이용을 막지 않는다")
    void preservesGapBetweenSelectedSlots() {
        var later = slot(room, 14, 16);
        blackout(room, selected, at(12), at(14));
        flush();
        assertThat(
                        list(
                                        Clock.fixed(BOOKED, ZONE),
                                        DATE,
                                        Set.of(selected.getId(), later.getId()))
                                .availableSeatIds())
                .containsExactly(seat.getId());
    }

    @Test
    @DisplayName("등록 슬롯이 다르고 폐기되었어도 점검은 방의 다른 슬롯 이용에 적용된다")
    void appliesRoomRestrictionFromRetiredRegistrationSlot() {
        var registration = slot(room, 14, 16);
        blackout(room, registration, at(10), at(12));
        registration.delete(BOOKED);
        flush();
        var clock = Clock.fixed(BOOKED, ZONE);

        assertThat(list(clock, DATE, Set.of(selected.getId())).availableSeatIds()).isEmpty();
        assertThatThrownBy(() -> reserve(clock, DATE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("삭제된 점검 구간은 실제 이용 구간과 겹쳐도 제외한다")
    void ignoresDeletedBlackout() {
        var deleted = blackout(room, selected, at(10), at(12));
        deleted.delete(BOOKED);
        flush();
        assertThat(
                        blackouts.existsOverlappingUsage(
                                room.getId(), Set.of(selected.getId()), DATE, ZONE))
                .isFalse();
    }

    @Test
    @DisplayName("다른 방의 점검은 같은 이용 시간의 예약을 막지 않는다")
    void ignoresOtherRoomBlackout() {
        var other =
                em.persist(Room.of(RoomId.of(UUID.randomUUID()), "ROOM-02", "Other", "", CREATED));
        blackout(other, slot(other, 10, 12), at(10), at(12));
        flush();
        assertThat(
                        blackouts.existsOverlappingUsage(
                                room.getId(), Set.of(selected.getId()), DATE, ZONE))
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({"Asia/Seoul,true", "UTC,false"})
    @DisplayName("이용 날짜와 로컬 슬롯 시간을 예약 시간대로 변환하여 점검 구간과 비교한다")
    void interpretsServiceDateInReservationTimezone(String zone, boolean blocked) {
        blackout(room, selected, at(10), at(12));
        flush();
        assertThat(
                        blackouts.existsOverlappingUsage(
                                room.getId(), Set.of(selected.getId()), DATE, ZoneId.of(zone)))
                .isEqualTo(blocked);
    }

    private com.lilamaris.lilserve.reservation.application.port.in.result.ListAvailableSeatResult
            list(Clock clock, LocalDate date, Set<ScheduleId> ids) {
        return new ListAvailableSeatService(rooms, availableSeats, blackouts, clock)
                .list(ListAvailableSeatQuery.of(room.getId(), date, ids));
    }

    private com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult
            reserve(Clock clock, LocalDate date) {
        return new CreateReservationService(
                        reservations,
                        allocations,
                        rooms,
                        seats,
                        schedules,
                        blackouts,
                        UUID::randomUUID,
                        clock)
                .create(
                        CreateReservationCommand.of(
                                room.getId(), seat.getId(), Set.of(selected.getId()), date));
    }

    private Schedule slot(Room owner, int start, int end) {
        return em.persist(
                Schedule.of(
                        ScheduleId.of(UUID.randomUUID()),
                        owner.getId(),
                        DATE.getDayOfWeek(),
                        LocalTime.of(start, 0),
                        LocalTime.of(end, 0),
                        CREATED));
    }

    private BlackoutSchedule blackout(
            Room owner, Schedule registration, Instant start, Instant end) {
        return em.persist(
                BlackoutSchedule.of(
                        BlackoutScheduleId.of(UUID.randomUUID()),
                        owner.getId(),
                        registration.getId(),
                        "Maintenance",
                        start,
                        end,
                        CREATED));
    }

    private Instant at(int hour) {
        return DATE.atTime(hour, 0).atZone(ZONE).toInstant();
    }

    private void flush() {
        em.flush();
        em.clear();
    }
}
