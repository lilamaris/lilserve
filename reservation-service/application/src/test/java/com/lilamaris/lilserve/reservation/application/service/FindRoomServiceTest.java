package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
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
@DisplayName("룸 조회 유스케이스")
class FindRoomServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final RoomId ROOM_ID = RoomId.of(ID);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";

    @Mock private RoomFindPort roomFindPort;

    private FindRoomService service;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new FindRoomService(roomFindPort);
        room = Room.of(ID, CODE, NAME, DESCRIPTION, CREATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 룸을 조회하고 마지막 변경까지 그대로 반환한다")
    void findsRoomAndReturnsResult() {
        room.updateName("small hall", UPDATED_AT);
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.of(room));

        var result = service.find(FindRoomQuery.of(ID));

        assertThat(result)
                .isEqualTo(
                        new FindRoomResult(
                                ROOM_ID,
                                CODE,
                                "small hall",
                                DESCRIPTION,
                                CREATED_AT,
                                UPDATED_AT,
                                null));
    }

    @Test
    @DisplayName("변경 이력이 없는 대상을 조회하면 변경 시각은 비어 있다")
    void findsTargetWithoutUpdateHistory() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.of(room));

        var result = service.find(FindRoomQuery.of(ID));

        assertThat(result)
                .isEqualTo(
                        new FindRoomResult(
                                ROOM_ID, CODE, NAME, DESCRIPTION, CREATED_AT, null, null));
    }

    @Test
    @DisplayName("대상 룸이 없으면 조회 실패 예외를 전파한다")
    void rejectsMissingRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(FindRoomQuery.of(ID)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("룸 조회가 실패하면 해당 예외를 전파한다")
    void propagatesFindFailure() {
        var failure = new IllegalStateException("Room lookup failed");
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.find(FindRoomQuery.of(ID))).isSameAs(failure);
    }
}
