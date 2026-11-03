package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸 생성 유스케이스")
class CreateRoomServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";

    @Mock private RoomSavePort roomSavePort;

    private CreateRoomService service;

    @BeforeEach
    void setUp() {
        service = new CreateRoomService(roomSavePort, () -> ID, CLOCK);
    }

    @Test
    @DisplayName("생성한 식별자와 현재 시각으로 룸을 저장하고 생성 결과를 반환한다")
    void savesRoomAndReturnsCreatedResult() {
        var command = CreateRoomCommand.of(CODE, NAME, DESCRIPTION);

        var result = service.create(command);

        var room = savedRoom();
        assertThat(room.getId()).isEqualTo(RoomId.of(ID));
        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(room.getUpdatedAt()).isNull();
        assertThat(room.getDeletedAt()).isNull();
        assertThat(result)
                .isEqualTo(
                        new CreateRoomResult(RoomId.of(ID), CODE, NAME, DESCRIPTION, CREATED_AT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("빈 설명도 그대로 저장하고 생성 결과에 반영한다")
    void acceptsBlankDescription(String description) {
        var result = service.create(CreateRoomCommand.of(CODE, NAME, description));

        assertThat(savedRoom().getDescription()).isEqualTo(description);
        assertThat(result.description()).isEqualTo(description);
    }

    @Test
    @DisplayName("문자열 길이가 상한과 같으면 룸을 저장하고 생성 결과를 반환한다")
    void acceptsValuesAtLengthLimits() {
        var code = "a".repeat(Room.MAX_CODE_LENGTH);
        var name = "b".repeat(Room.MAX_NAME_LENGTH);
        var description = "c".repeat(Room.MAX_DESCRIPTION_LENGTH);

        var result = service.create(CreateRoomCommand.of(code, name, description));

        var room = savedRoom();
        assertThat(room.getCode()).isEqualTo(code);
        assertThat(room.getName()).isEqualTo(name);
        assertThat(room.getDescription()).isEqualTo(description);
        assertThat(result)
                .isEqualTo(
                        new CreateRoomResult(RoomId.of(ID), code, name, description, CREATED_AT));
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    @DisplayName("도메인이 커맨드를 거부하면 예외를 전파하고 룸을 저장하지 않는다")
    void doesNotSaveWhenDomainRejectsCommand(
            CreateRoomCommand command, Class<? extends Throwable> exceptionType) {
        assertThatThrownBy(() -> service.create(command)).isInstanceOf(exceptionType);

        verifyNoInteractions(roomSavePort);
    }

    @Test
    @DisplayName("식별자 생성이 실패하면 예외를 전파하고 룸을 저장하지 않는다")
    void propagatesIdGenerationFailureWithoutSaving() {
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                new CreateRoomService(
                        roomSavePort,
                        () -> {
                            throw failure;
                        },
                        CLOCK);

        assertThatThrownBy(() -> service.create(CreateRoomCommand.of(CODE, NAME, DESCRIPTION)))
                .isSameAs(failure);

        verifyNoInteractions(roomSavePort);
    }

    @Test
    @DisplayName("룸 저장이 실패하면 생성 결과 대신 저장 예외를 전파한다")
    void propagatesSaveFailure() {
        var failure = new IllegalStateException("Room storage failed");
        doThrow(failure).when(roomSavePort).save(any(Room.class));

        assertThatThrownBy(() -> service.create(CreateRoomCommand.of(CODE, NAME, DESCRIPTION)))
                .isSameAs(failure);
    }

    private Room savedRoom() {
        var captor = ArgumentCaptor.forClass(Room.class);
        verify(roomSavePort).save(captor.capture());
        return captor.getValue();
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                arguments(
                        CreateRoomCommand.of(null, NAME, DESCRIPTION), NullPointerException.class),
                arguments(
                        CreateRoomCommand.of(CODE, null, DESCRIPTION), NullPointerException.class),
                arguments(CreateRoomCommand.of(CODE, NAME, null), NullPointerException.class),
                arguments(
                        CreateRoomCommand.of("", NAME, DESCRIPTION),
                        IllegalValueConditionException.class),
                arguments(
                        CreateRoomCommand.of(" ", NAME, DESCRIPTION),
                        IllegalValueConditionException.class),
                arguments(
                        CreateRoomCommand.of(CODE, "", DESCRIPTION),
                        IllegalValueConditionException.class),
                arguments(
                        CreateRoomCommand.of(CODE, " ", DESCRIPTION),
                        IllegalValueConditionException.class),
                arguments(
                        CreateRoomCommand.of(
                                "a".repeat(Room.MAX_CODE_LENGTH + 1), NAME, DESCRIPTION),
                        IllegalComparisonConditionException.class),
                arguments(
                        CreateRoomCommand.of(
                                CODE, "b".repeat(Room.MAX_NAME_LENGTH + 1), DESCRIPTION),
                        IllegalComparisonConditionException.class),
                arguments(
                        CreateRoomCommand.of(
                                CODE, NAME, "c".repeat(Room.MAX_DESCRIPTION_LENGTH + 1)),
                        IllegalComparisonConditionException.class));
    }
}
