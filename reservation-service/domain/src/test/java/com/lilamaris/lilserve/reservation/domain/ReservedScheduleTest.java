package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ReservedScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("좌석 예약 슬롯")
class ReservedScheduleTest {
    private static final UUID ALLOCATION_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final LocalDate RESERVATION_DATE = LocalDate.of(2026, Month.NOVEMBER, 9);
    private static final String TIME_ZONE = "Asia/Seoul";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final LocalTime START_AT = LocalTime.of(10, 0);
    private static final LocalTime END_AT = LocalTime.of(12, 0);

    private ReservedSchedule allocation() {
        return ReservedSchedule.of(
                ReservedScheduleId.of(ALLOCATION_ID),
                ReservationId.of(RESERVATION_ID),
                RoomId.of(ROOM_ID),
                SeatId.of(SEAT_ID),
                ScheduleId.of(SCHEDULE_ID),
                START_AT,
                END_AT,
                RESERVATION_DATE,
                TIME_ZONE,
                CREATED_AT);
    }

    @Test
    @DisplayName("예약 슬롯은 입력한 값을 그대로 유지한다")
    void retainsSuppliedValues() {
        var allocation = allocation();

        assertThat(allocation.getId().getValue()).isEqualTo(ALLOCATION_ID);
        assertThat(allocation.getReservationId().getValue()).isEqualTo(RESERVATION_ID);
        assertThat(allocation.getRoomId().getValue()).isEqualTo(ROOM_ID);
        assertThat(allocation.getSeatId().getValue()).isEqualTo(SEAT_ID);
        assertThat(allocation.getScheduleId().getValue()).isEqualTo(SCHEDULE_ID);
        assertThat(allocation.getStartAt()).isEqualTo(START_AT);
        assertThat(allocation.getEndAt()).isEqualTo(END_AT);
        assertThat(allocation.getReservationDate()).isEqualTo(RESERVATION_DATE);
        assertThat(allocation.getTimeZone()).isEqualTo(TIME_ZONE);
        assertThat(allocation.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(allocation.getDeletedAt()).isNull();
        assertThat(allocation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("절대 시각과 요일은 생성 시점에는 비어 있다")
    void leavesGeneratedColumnsUnsetBeforeInsert() {
        var allocation = allocation();

        assertThat(allocation.getStartsAt()).isNull();
        assertThat(allocation.getEndsAt()).isNull();
        assertThat(allocation.getDayOfWeek()).isNull();
    }

    @Test
    @DisplayName("저장된 날짜와 시각으로 시작 절대 시각을 계산한다")
    void computesStartInstantFromStoredSnapshot() {
        var allocation = allocation();

        assertThat(allocation.startInstant()).isEqualTo(Instant.parse("2026-11-09T01:00:00Z"));
    }

    @Test
    @DisplayName("저장된 날짜와 시각으로 종료 절대 시각을 계산한다")
    void computesEndInstantFromStoredSnapshot() {
        var allocation = allocation();

        assertThat(allocation.endInstant()).isEqualTo(Instant.parse("2026-11-09T03:00:00Z"));
    }

    @Test
    @DisplayName("시각이 겹치는 범위는 겹침으로 판단한다")
    void detectsOverlappingInterval() {
        var allocation = allocation();

        assertThat(
                        allocation.overlaps(
                                Instant.parse("2026-11-09T02:00:00Z"),
                                Instant.parse("2026-11-09T04:00:00Z")))
                .isTrue();
    }

    @Test
    @DisplayName("정확히 종료 시각부터 시작하는 범위는 겹치지 않는다")
    void acceptsIntervalStartingAtEnd() {
        var allocation = allocation();

        assertThat(
                        allocation.overlaps(
                                Instant.parse("2026-11-09T03:00:00Z"),
                                Instant.parse("2026-11-09T04:00:00Z")))
                .isFalse();
    }

    @Test
    @DisplayName("같은 좌석의 겹치는 예약 슬롯은 겹침으로 판단한다")
    void detectsOverlapOnSameSeat() {
        var first = allocation();
        var second =
                ReservedSchedule.of(
                        ReservedScheduleId.of(UUID.randomUUID()),
                        ReservationId.of(RESERVATION_ID),
                        RoomId.of(ROOM_ID),
                        SeatId.of(SEAT_ID),
                        ScheduleId.of(UUID.randomUUID()),
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        RESERVATION_DATE,
                        TIME_ZONE,
                        CREATED_AT);

        assertThat(first.overlaps(second)).isTrue();
    }

    @Test
    @DisplayName("다른 좌석의 겹치는 예약 슬롯은 겹침이 아니다")
    void ignoresOverlapOnOtherSeat() {
        var first = allocation();
        var second =
                ReservedSchedule.of(
                        ReservedScheduleId.of(UUID.randomUUID()),
                        ReservationId.of(RESERVATION_ID),
                        RoomId.of(ROOM_ID),
                        SeatId.of(UUID.randomUUID()),
                        ScheduleId.of(UUID.randomUUID()),
                        LocalTime.of(11, 0),
                        LocalTime.of(13, 0),
                        RESERVATION_DATE,
                        TIME_ZONE,
                        CREATED_AT);

        assertThat(first.overlaps(second)).isFalse();
    }

    @Test
    @DisplayName("종료 시각이 시작 시각과 같으면 예약 슬롯을 생성할 수 없다")
    void rejectsEndAtEqualToStartAt() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        START_AT,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("예약 식별자가 null이면 예약 슬롯을 생성할 수 없다")
    void rejectsNullReservationId() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        null,
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        END_AT,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("룸 식별자가 null이면 예약 슬롯을 생성할 수 없다")
    void rejectsNullRoomId() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        null,
                                        SeatId.of(SEAT_ID),
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        END_AT,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("좌석 식별자가 null이면 예약 슬롯을 생성할 수 없다")
    void rejectsNullSeatId() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        null,
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        END_AT,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("스케줄 식별자가 null이면 예약 슬롯을 생성할 수 없다")
    void rejectsNullScheduleId() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        null,
                                        START_AT,
                                        END_AT,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("서비스 날짜가 null이면 예약 슬롯을 생성할 수 없다")
    void rejectsNullReservationDate() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        END_AT,
                                        null,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("존재하지 않는 시간대이면 예약 슬롯을 생성할 수 없다")
    void rejectsUnknownTimeZone() {
        assertThatThrownBy(
                        () ->
                                ReservedSchedule.of(
                                        ReservedScheduleId.of(ALLOCATION_ID),
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        ScheduleId.of(SCHEDULE_ID),
                                        START_AT,
                                        END_AT,
                                        RESERVATION_DATE,
                                        "Mars/Phobos",
                                        CREATED_AT))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    @DisplayName("취소 시점 유지하면 취소된 예약 슬롯이 된다")
    void marksDeletedAtCancellation() {
        var allocation = allocation();

        allocation.delete(DELETED_AT);

        assertThat(allocation.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(allocation.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("취소 시점이 생성 시점보다 앞이면 취소할 수 없다")
    void rejectsCancellationBeforeCreatedAt() {
        var allocation = allocation();
        Instant beforeCreatedAt = CREATED_AT.minusSeconds(1);

        assertThatThrownBy(() -> allocation.delete(beforeCreatedAt))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(allocation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 취소된 예약 슬롯은 다시 취소할 수 없다")
    void rejectsSecondCancellation() {
        var allocation = allocation();
        allocation.delete(DELETED_AT);

        assertThatThrownBy(() -> allocation.delete(DELETED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(allocation.getDeletedAt()).isEqualTo(DELETED_AT);
    }

    @Test
    @DisplayName("취소되지 않은 예약 슬롯은 사용 검증에 통과한다")
    void passesNotDeletedValidationWhenNotDeleted() {
        var allocation = allocation();

        allocation.validateNotDeleted();

        assertThat(allocation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("취소된 예약 슬롯은 사용 검증에 통과하지 못한다")
    void failsNotDeletedValidationWhenDeleted() {
        var allocation = allocation();
        allocation.delete(DELETED_AT);

        assertThatThrownBy(allocation::validateNotDeleted)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("취소된 예약 슬롯은 취소 검증에 통과한다")
    void passesDeletedValidationWhenDeleted() {
        var allocation = allocation();
        allocation.delete(DELETED_AT);

        allocation.validateDeleted();

        assertThat(allocation.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("취소되지 않은 예약 슬롯은 취소 검증에 통과하지 못한다")
    void failsDeletedValidationWhenNotDeleted() {
        var allocation = allocation();

        assertThatThrownBy(allocation::validateDeleted).isInstanceOf(IllegalStateException.class);
    }
}
