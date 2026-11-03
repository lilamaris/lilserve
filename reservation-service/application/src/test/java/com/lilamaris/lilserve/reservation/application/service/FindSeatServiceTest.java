package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("좌석 조회 유스케이스")
class FindSeatServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final SeatId SEAT_ID = SeatId.of(ID);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final String CODE = "A-01";
    private static final UUID ROOM_VALUE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock private SeatFindPort seatFindPort;

    private FindSeatService service;
    private Seat seat;

    @BeforeEach
    void setUp() {
        service = new FindSeatService(seatFindPort);
        seat = Seat.of(ID, ROOM_VALUE, CODE, CREATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 좌석을 조회하고 마지막 변경까지 그대로 반환한다")
    void findsSeatAndReturnsResult() {
        seat.updateCode("A-02", UPDATED_AT);
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenReturn(Optional.of(seat));

        var result = service.find(FindSeatQuery.of(ID));

        assertThat(result)
                .isEqualTo(
                        new FindSeatResult(
                                SEAT_ID,
                                RoomId.of(ROOM_VALUE),
                                "A-02",
                                CREATED_AT,
                                UPDATED_AT,
                                null));
    }

    @Test
    @DisplayName("변경 이력이 없는 대상을 조회하면 변경 시각은 비어 있다")
    void findsTargetWithoutUpdateHistory() {
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenReturn(Optional.of(seat));

        var result = service.find(FindSeatQuery.of(ID));

        assertThat(result)
                .isEqualTo(
                        new FindSeatResult(
                                SEAT_ID, RoomId.of(ROOM_VALUE), CODE, CREATED_AT, null, null));
    }

    @Test
    @DisplayName("대상 좌석이 없으면 조회 실패 예외를 전파한다")
    void rejectsMissingSeat() {
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(FindSeatQuery.of(ID)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("좌석 조회가 실패하면 해당 예외를 전파한다")
    void propagatesFindFailure() {
        var failure = new IllegalStateException("Seat lookup failed");
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.find(FindSeatQuery.of(ID))).isSameAs(failure);
    }
}
