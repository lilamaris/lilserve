package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.port.in.query.ListAvailableSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.out.AvailableSeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 가능한 좌석 조회 유스케이스")
class ListAvailableSeatServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-13T01:00:00Z");
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 20);
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    private static final Set<ScheduleId> SCHEDULE_IDS =
            Set.of(ScheduleId.of(UUID.randomUUID()), ScheduleId.of(UUID.randomUUID()));

    @Mock private RoomFindPort roomFindPort;
    @Mock private AvailableSeatFindPort availableSeatFindPort;
    @Mock private BlackoutScheduleFindPort blackoutScheduleFindPort;
    private ListAvailableSeatService service;

    @BeforeEach
    void setUp() {
        service =
                new ListAvailableSeatService(
                        roomFindPort,
                        availableSeatFindPort,
                        blackoutScheduleFindPort,
                        Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
    }

    @Test
    @DisplayName("선택 슬롯 중 현재 접수가 차단된 슬롯이 있으면 이용 날짜와 무관하게 빈 좌석 목록을 반환한다")
    void returnsNoSeatsDuringActiveIntakeBlackout() {
        givenRoom();
        when(blackoutScheduleFindPort.existsActiveByScheduleIdsAt(SCHEDULE_IDS, NOW))
                .thenReturn(true);

        var result = service.list(ListAvailableSeatQuery.of(ROOM_ID, TARGET_DATE, SCHEDULE_IDS));

        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.targetDate()).isEqualTo(TARGET_DATE);
        assertThat(result.availableSeatIds()).isEmpty();
        verifyNoInteractions(availableSeatFindPort);
    }

    @Test
    @DisplayName("현재 선택 슬롯의 접수가 차단되지 않았으면 조회한 좌석 식별자를 반환한다")
    void returnsAvailableSeatsWhenIntakeIsOpen() {
        givenRoom();
        when(blackoutScheduleFindPort.existsActiveByScheduleIdsAt(SCHEDULE_IDS, NOW))
                .thenReturn(false);
        var seat = Seat.of(SeatId.of(UUID.randomUUID()), ROOM_ID, "A-01", NOW);
        when(availableSeatFindPort.findAvailableSeat(
                        ROOM_ID, TARGET_DATE, SCHEDULE_IDS, ZoneId.of("Asia/Seoul")))
                .thenReturn(List.of(seat));

        var result = service.list(ListAvailableSeatQuery.of(ROOM_ID, TARGET_DATE, SCHEDULE_IDS));

        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.targetDate()).isEqualTo(TARGET_DATE);
        assertThat(result.availableSeatIds()).containsExactly(seat.getId());
    }

    @Test
    @DisplayName("선택한 슬롯이 없으면 차단 여부나 좌석을 조회하지 않고 빈 목록을 반환한다")
    void returnsNoSeatsForEmptySelection() {
        givenRoom();

        var result = service.list(ListAvailableSeatQuery.of(ROOM_ID, TARGET_DATE, Set.of()));

        assertThat(result.availableSeatIds()).isEmpty();
        verifyNoInteractions(blackoutScheduleFindPort, availableSeatFindPort);
    }

    @Test
    @DisplayName("방이 없거나 삭제되었으면 예약 가능한 좌석 조회를 거부한다")
    void rejectsMissingRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.list(
                                        ListAvailableSeatQuery.of(
                                                ROOM_ID, TARGET_DATE, SCHEDULE_IDS)))
                .isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(blackoutScheduleFindPort, availableSeatFindPort);
    }

    private void givenRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID))
                .thenReturn(Optional.of(Room.of(ROOM_ID, "ROOM-01", "Main hall", "", NOW)));
    }
}
