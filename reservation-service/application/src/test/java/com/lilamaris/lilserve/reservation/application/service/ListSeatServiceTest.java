package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.util.List;
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
@DisplayName("좌석 목록 조회 유스케이스")
class ListSeatServiceTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    @Mock private SeatFindPort seatFindPort;
    @Mock private RoomFindPort roomFindPort;
    private ListSeatService service;

    @BeforeEach
    void setUp() {
        service = new ListSeatService(seatFindPort, roomFindPort);
    }

    @Test
    @DisplayName("목록 항목을 결과로 변환하고 페이지 번호와 크기 및 전체 건수를 보존한다")
    void mapsItemsAndPreservesPageMetadata() {
        givenRoom();
        var entity = Seat.of(SeatId.of(UUID.randomUUID()), ROOM_ID, "A-01", CREATED);
        var query = ListSeatQuery.of(ROOM_ID, OffsetPageRequest.of(1, 2));
        when(seatFindPort.findNotDeletedByRoomId(ROOM_ID, query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(entity), 1, 2, 3));

        var result = service.list(query);

        assertThat(result.items()).containsExactly(FindSeatResult.from(entity));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("범위를 벗어난 페이지는 빈 목록과 기존 전체 건수를 반환한다")
    void preservesTotalForEmptyPage() {
        givenRoom();
        var query = ListSeatQuery.of(ROOM_ID, OffsetPageRequest.of(3, 2));
        when(seatFindPort.findNotDeletedByRoomId(ROOM_ID, query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(), 3, 2, 3));

        var result = service.list(query);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("방이 없거나 삭제되어 조회되지 않으면 좌석 목록 조회를 거부한다")
    void rejectsMissingRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.list(ListSeatQuery.of(ROOM_ID, OffsetPageRequest.of(0, 10))))
                .isInstanceOf(NoSuchElementException.class);
    }

    private void givenRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID))
                .thenReturn(Optional.of(Room.of(ROOM_ID, "ROOM-01", "Main hall", "", CREATED)));
    }
}
