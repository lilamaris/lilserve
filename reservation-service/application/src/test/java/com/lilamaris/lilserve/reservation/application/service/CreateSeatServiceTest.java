package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("좌석 생성 유스케이스")
class CreateSeatServiceTest {
    private static final UUID SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
    private static final String CODE = "A-01";

    @Mock private SeatSavePort seatSavePort;
    @Mock private RoomFindPort roomFindPort;

    private CreateSeatService service;

    @BeforeEach
    void setUp() {
        service = new CreateSeatService(seatSavePort, roomFindPort, () -> SEAT_ID, CLOCK);
    }

    @Test
    @DisplayName("부모 룸이 있으면 생성한 식별자와 현재 시각으로 좌석를 저장하고 생성 결과를 반환한다")
    void savesSeatAndReturnsCreatedResult() {
        givenRoomFound();

        var result = service.create(CreateSeatCommand.of(ROOM_ID, CODE));

        var seat = savedSeat();
        assertThat(seat.getId()).isEqualTo(SeatId.of(SEAT_ID));
        assertThat(seat.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(seat.getUpdatedAt()).isNull();
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(result)
                .isEqualTo(
                        new CreateSeatResult(
                                SeatId.of(SEAT_ID), RoomId.of(ROOM_ID), CODE, CREATED_AT));
    }

    @Test
    @DisplayName("코드 길이가 상한과 같으면 좌석를 저장하고 생성 결과를 반환한다")
    void acceptsCodeAtMaxLength() {
        givenRoomFound();
        var code = "a".repeat(Seat.MAX_CODE_LENGTH);

        var result = service.create(CreateSeatCommand.of(ROOM_ID, code));

        assertThat(savedSeat().getCode()).isEqualTo(code);
        assertThat(result.code()).isEqualTo(code);
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    @DisplayName("도메인이 커맨드를 거부하면 예외를 전파하고 좌석를 저장하지 않는다")
    void doesNotSaveWhenDomainRejectsCommand(
            CreateSeatCommand command, Class<? extends Throwable> exceptionType) {
        givenRoomFound();

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(exceptionType);

        verifyNoInteractions(seatSavePort);
    }

    @Test
    @DisplayName("부모 룸가 없으면 조회 실패 예외를 전파하고 좌석를 저장하지 않는다")
    void rejectsMissingRoom() {
        when(roomFindPort.findNotDeletedById(RoomId.of(ROOM_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CreateSeatCommand.of(ROOM_ID, CODE)))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(seatSavePort);
    }

    @Test
    @DisplayName("부모 룸 조회가 실패하면 해당 예외를 전파하고 좌석를 저장하지 않는다")
    void propagatesRoomFindFailure() {
        var failure = new IllegalStateException("Room lookup failed");
        when(roomFindPort.findNotDeletedById(RoomId.of(ROOM_ID))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(CreateSeatCommand.of(ROOM_ID, CODE)))
                .isSameAs(failure);

        verifyNoInteractions(seatSavePort);
    }

    @Test
    @DisplayName("식별자 생성이 실패하면 예외를 전파하고 좌석를 저장하지 않는다")
    void propagatesIdGenerationFailureWithoutSaving() {
        givenRoomFound();
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                new CreateSeatService(
                        seatSavePort,
                        roomFindPort,
                        () -> {
                            throw failure;
                        },
                        CLOCK);

        assertThatThrownBy(() -> service.create(CreateSeatCommand.of(ROOM_ID, CODE)))
                .isSameAs(failure);

        verifyNoInteractions(seatSavePort);
    }

    @Test
    @DisplayName("좌석 저장이 실패하면 생성 결과 대신 저장 예외를 전파한다")
    void propagatesSaveFailure() {
        givenRoomFound();
        var failure = new IllegalStateException("Seat storage failed");
        doThrow(failure).when(seatSavePort).save(any(Seat.class));

        assertThatThrownBy(() -> service.create(CreateSeatCommand.of(ROOM_ID, CODE)))
                .isSameAs(failure);
    }

    private void givenRoomFound() {
        var room = Room.of(ROOM_ID, "ROOM-01", "main hall", "capacity 100", CREATED_AT);
        when(roomFindPort.findNotDeletedById(RoomId.of(ROOM_ID))).thenReturn(Optional.of(room));
    }

    private Seat savedSeat() {
        var captor = ArgumentCaptor.forClass(Seat.class);
        verify(seatSavePort).save(captor.capture());
        return captor.getValue();
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                arguments(CreateSeatCommand.of(ROOM_ID, null), NullPointerException.class),
                arguments(CreateSeatCommand.of(ROOM_ID, ""), IllegalValueConditionException.class),
                arguments(CreateSeatCommand.of(ROOM_ID, " "), IllegalValueConditionException.class),
                arguments(
                        CreateSeatCommand.of(ROOM_ID, "a".repeat(Seat.MAX_CODE_LENGTH + 1)),
                        IllegalComparisonConditionException.class));
    }
}
