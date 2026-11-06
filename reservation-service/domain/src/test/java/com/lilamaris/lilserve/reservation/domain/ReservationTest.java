package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("예약 헤더")
class ReservationTest {
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final LocalDate RESERVATION_DATE = LocalDate.of(2026, Month.NOVEMBER, 9);
    private static final String TIME_ZONE = "Asia/Seoul";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");

    private Reservation reservation() {
        return Reservation.of(
                ReservationId.of(RESERVATION_ID),
                RoomId.of(ROOM_ID),
                SeatId.of(SEAT_ID),
                RESERVATION_DATE,
                TIME_ZONE,
                CREATED_AT);
    }

    @Test
    @DisplayName("예약은 입력한 범위를 그대로 유지한다")
    void retainsSuppliedScope() {
        var reservation = reservation();

        assertThat(reservation.getId().getValue()).isEqualTo(RESERVATION_ID);
        assertThat(reservation.getRoomId().getValue()).isEqualTo(ROOM_ID);
        assertThat(reservation.getSeatId().getValue()).isEqualTo(SEAT_ID);
        assertThat(reservation.getReservationDate()).isEqualTo(RESERVATION_DATE);
        assertThat(reservation.getTimeZone()).isEqualTo(TIME_ZONE);
        assertThat(reservation.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(reservation.getDeletedAt()).isNull();
        assertThat(reservation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("시간대는 ZoneId로 읽을 수 있다")
    void exposesTimeZoneAsZoneId() {
        var reservation = reservation();

        assertThat(reservation.zone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("예약은 하나의 좌석 범위만 표현한다")
    void keepsOneSeatScope() {
        var reservation = reservation();

        assertThat(reservation.getSeatId().getValue()).isEqualTo(SEAT_ID);
    }

    @Test
    @DisplayName("유효한 시간대는 정규화되어 저장된다")
    void normalizesTimeZoneId() {
        var reservation =
                Reservation.of(
                        ReservationId.of(RESERVATION_ID),
                        RoomId.of(ROOM_ID),
                        SeatId.of(SEAT_ID),
                        RESERVATION_DATE,
                        "Z",
                        CREATED_AT);

        assertThat(reservation.getTimeZone()).isEqualTo("Z");
        assertThat(reservation.zone()).isEqualTo(ZoneId.of("Z"));
    }

    @Test
    @DisplayName("예약 식별자가 null이면 예약을 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        null,
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("룸 식별자가 null이면 예약을 생성할 수 없다")
    void rejectsNullRoomId() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        null,
                                        SeatId.of(SEAT_ID),
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("좌석 식별자가 null이면 예약을 생성할 수 없다")
    void rejectsNullSeatId() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        null,
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("서비스 날짜가 null이면 예약을 생성할 수 없다")
    void rejectsNullReservationDate() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        null,
                                        TIME_ZONE,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("시간대가 null이면 예약을 생성할 수 없다")
    void rejectsNullTimeZone() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        RESERVATION_DATE,
                                        null,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("존재하지 않는 시간대이면 예약을 생성할 수 없다")
    void rejectsUnknownTimeZone() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        RESERVATION_DATE,
                                        "Asia/Seoull",
                                        CREATED_AT))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    @DisplayName("생성 시점이 null이면 예약을 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(
                        () ->
                                Reservation.of(
                                        ReservationId.of(RESERVATION_ID),
                                        RoomId.of(ROOM_ID),
                                        SeatId.of(SEAT_ID),
                                        RESERVATION_DATE,
                                        TIME_ZONE,
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("취소 시점 유지하면 취소된 예약이 된다")
    void marksDeletedAtCancellation() {
        var reservation = reservation();

        reservation.delete(DELETED_AT);

        assertThat(reservation.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(reservation.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("취소 시점이 생성 시점보다 앞이면 취소할 수 없다")
    void rejectsCancellationBeforeCreatedAt() {
        var reservation = reservation();
        Instant beforeCreatedAt = CREATED_AT.minusSeconds(1);

        assertThatThrownBy(() -> reservation.delete(beforeCreatedAt))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(reservation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("취소 시점이 null이면 취소할 수 없다")
    void rejectsNullDeletedAt() {
        var reservation = reservation();

        assertThatThrownBy(() -> reservation.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(reservation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void rejectsSecondCancellation() {
        var reservation = reservation();
        reservation.delete(DELETED_AT);

        assertThatThrownBy(() -> reservation.delete(DELETED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(reservation.getDeletedAt()).isEqualTo(DELETED_AT);
    }

    @Test
    @DisplayName("취소되지 않은 예약은 사용 검증에 통과한다")
    void passesNotDeletedValidationWhenNotDeleted() {
        var reservation = reservation();

        reservation.validateNotDeleted();

        assertThat(reservation.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("취소된 예약은 사용 검증에 통과하지 못한다")
    void failsNotDeletedValidationWhenDeleted() {
        var reservation = reservation();
        reservation.delete(DELETED_AT);

        assertThatThrownBy(reservation::validateNotDeleted)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("취소된 예약은 취소 검증에 통과한다")
    void passesDeletedValidationWhenDeleted() {
        var reservation = reservation();
        reservation.delete(DELETED_AT);

        reservation.validateDeleted();

        assertThat(reservation.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("취소되지 않은 예약은 취소 검증에 통과하지 못한다")
    void failsDeletedValidationWhenNotDeleted() {
        var reservation = reservation();

        assertThatThrownBy(reservation::validateDeleted).isInstanceOf(IllegalStateException.class);
    }
}
