package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("룸 변경 유스케이스")
class UpdateRoomServiceTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final RoomId ROOM_ID = RoomId.of(ID);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED_AT.plusSeconds(60);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";
    private static final String NEW_CODE = "ROOM-02";
    private static final String NEW_NAME = "small hall";
    private static final String NEW_DESCRIPTION = "capacity 20";

    @Mock private RoomFindPort roomFindPort;
    @Mock private RoomSavePort roomSavePort;

    private UpdateRoomService service;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new UpdateRoomService(roomFindPort, roomSavePort, CLOCK);
        room = activeRoom();
    }

    @Test
    @DisplayName("코드를 변경하여 저장하고 현재 시각이 반영된 결과를 반환한다")
    void updatesCodeAndReturnsSavedRoom() {
        givenRoomFound(UPDATE_CODE);

        var result = service.updateCode(UpdateRoomCodeCommand.of(ID, NEW_CODE));

        verify(roomSavePort).save(room);
        assertThat(room.getCode()).isEqualTo(NEW_CODE);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result)
                .isEqualTo(
                        new UpdateRoomResult(
                                ROOM_ID, NEW_CODE, NAME, DESCRIPTION, CREATED_AT, NOW, null));
    }

    @Test
    @DisplayName("이름과 설명을 함께 변경하여 저장하고 변경 결과를 반환한다")
    void updatesMetadataAndReturnsSavedRoom() {
        givenRoomFound(UPDATE_METADATA);

        var result =
                service.updateMetadata(UpdateRoomMetadataCommand.of(ID, NEW_NAME, NEW_DESCRIPTION));

        verify(roomSavePort).save(room);
        assertThat(room.getName()).isEqualTo(NEW_NAME);
        assertThat(room.getDescription()).isEqualTo(NEW_DESCRIPTION);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result)
                .isEqualTo(
                        new UpdateRoomResult(
                                ROOM_ID, CODE, NEW_NAME, NEW_DESCRIPTION, CREATED_AT, NOW, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("빈 설명으로도 메타데이터를 변경할 수 있다")
    void acceptsBlankDescription(String description) {
        givenRoomFound(UPDATE_METADATA);

        var result = service.updateMetadata(UpdateRoomMetadataCommand.of(ID, NAME, description));

        verify(roomSavePort).save(room);
        assertThat(room.getDescription()).isEqualTo(description);
        assertThat(result.description()).isEqualTo(description);
    }

    @Test
    @DisplayName("룸을 삭제하여 저장하고 삭제 시각이 반영된 결과를 반환한다")
    void deletesRoomAndReturnsSavedRoom() {
        givenRoomFound(DELETE);

        var result = service.delete(DeleteRoomCommand.of(ID));

        verify(roomSavePort).save(room);
        assertThat(room.isDeleted()).isTrue();
        assertThat(room.getDeletedAt()).isEqualTo(NOW);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result)
                .isEqualTo(
                        new UpdateRoomResult(
                                ROOM_ID, CODE, NAME, DESCRIPTION, CREATED_AT, NOW, NOW));
    }

    @Test
    @DisplayName("삭제된 룸을 복구하여 저장하고 복원 시각이 반영된 결과를 반환한다")
    void restoresDeletedRoomAndReturnsSavedRoom() {
        room = deletedRoom(NOW.minusSeconds(30));
        givenRoomFound(RESTORE);

        var result = service.restore(RestoreRoomCommand.of(ID));

        verify(roomSavePort).save(room);
        assertThat(room.isDeleted()).isFalse();
        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result)
                .isEqualTo(
                        new UpdateRoomResult(
                                ROOM_ID, CODE, NAME, DESCRIPTION, CREATED_AT, NOW, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("대상 룸이 없으면 조회 실패 예외를 전파하고 저장하지 않는다")
    void rejectsMissingRoom(String name, Operation operation) {
        operation.givenRoom(roomFindPort, Optional.empty());

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(roomSavePort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("룸 조회가 실패하면 해당 예외를 전파하고 저장하지 않는다")
    void propagatesFindFailure(String name, Operation operation) {
        var failure = new IllegalStateException("Room lookup failed");
        when(operation.lookup().apply(roomFindPort)).thenThrow(failure);

        assertThatThrownBy(() -> operation.apply().apply(service)).isSameAs(failure);

        verifyNoInteractions(roomSavePort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("룸 저장이 실패하면 변경 결과 대신 저장 예외를 전파한다")
    void propagatesSaveFailure(String name, Operation operation) {
        room = operation.roomWithLastUpdate().apply(NOW);
        operation.givenRoom(roomFindPort, Optional.of(room));
        var failure = new IllegalStateException("Room storage failed");
        doThrow(failure).when(roomSavePort).save(room);

        assertThatThrownBy(() -> operation.apply().apply(service)).isSameAs(failure);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("현재 시각이 마지막 변경보다 이르면 도메인 예외를 전파하고 저장하지 않는다")
    void rejectsTimeBeforeLastUpdate(String name, Operation operation) {
        room = operation.roomWithLastUpdate().apply(NOW.plusSeconds(1));
        operation.givenRoom(roomFindPort, Optional.of(room));

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(roomSavePort);
    }

    @ParameterizedTest
    @MethodSource("invalidCodes")
    @DisplayName("도메인이 코드를 거부하면 예외를 전파하고 저장하지 않는다")
    void rejectsInvalidCode(String code, Class<? extends Throwable> exceptionType) {
        givenRoomFound(UPDATE_CODE);

        assertThatThrownBy(() -> service.updateCode(UpdateRoomCodeCommand.of(ID, code)))
                .isInstanceOf(exceptionType);

        verifyNoInteractions(roomSavePort);
        assertThat(room.getCode()).isEqualTo(CODE);
    }

    @ParameterizedTest
    @MethodSource("invalidMetadata")
    @DisplayName("도메인이 메타데이터를 거부하면 예외를 전파하고 저장하지 않는다")
    void rejectsInvalidMetadata(
            String name, String description, Class<? extends Throwable> exceptionType) {
        givenRoomFound(UPDATE_METADATA);

        assertThatThrownBy(
                        () ->
                                service.updateMetadata(
                                        UpdateRoomMetadataCommand.of(ID, name, description)))
                .isInstanceOf(exceptionType);

        verifyNoInteractions(roomSavePort);
        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("마지막 변경 시각과 현재 시각이 같으면 변경을 저장한다")
    void acceptsTimeAtLastUpdate(String name, Operation operation) {
        room = operation.roomWithLastUpdate().apply(NOW);
        operation.givenRoom(roomFindPort, Optional.of(room));

        var result = operation.apply().apply(service);

        verify(roomSavePort).save(room);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대 길이의 코드로 변경할 수 있다")
    void acceptsCodeAtMaxLengthOnUpdate() {
        givenRoomFound(UPDATE_CODE);
        var code = "a".repeat(Room.MAX_CODE_LENGTH);

        var result = service.updateCode(UpdateRoomCodeCommand.of(ID, code));

        verify(roomSavePort).save(room);
        assertThat(result.code()).isEqualTo(code);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeOperations")
    @DisplayName("생성 시각과 현재 시각이 같으면 변경을 저장한다")
    void acceptsTimeAtCreation(String name, Operation operation) {
        service =
                new UpdateRoomService(
                        roomFindPort, roomSavePort, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
        room = activeRoom();
        operation.givenRoom(roomFindPort, Optional.of(room));

        var result = operation.apply().apply(service);

        verify(roomSavePort).save(room);
        assertThat(result.updatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeOperations")
    @DisplayName("현재 시각이 생성 시각보다 이르면 저장하지 않는다")
    void rejectsTimeBeforeCreation(String name, Operation operation) {
        service =
                new UpdateRoomService(
                        roomFindPort,
                        roomSavePort,
                        Clock.fixed(CREATED_AT.minusNanos(1), ZoneOffset.UTC));
        operation.givenRoom(roomFindPort, Optional.of(room));

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(roomSavePort);
        assertThat(room.getUpdatedAt()).isNull();
    }

    private static Stream<Arguments> activeOperations() {
        return Stream.of(
                arguments(UPDATE_CODE.name(), UPDATE_CODE),
                arguments(UPDATE_METADATA.name(), UPDATE_METADATA),
                arguments(DELETE.name(), DELETE));
    }

    @Test
    @DisplayName("이름과 설명의 최대 길이로 메타데이터를 변경할 수 있다")
    void acceptsMetadataAtLengthLimits() {
        givenRoomFound(UPDATE_METADATA);
        var name = "a".repeat(Room.MAX_NAME_LENGTH);
        var description = "b".repeat(Room.MAX_DESCRIPTION_LENGTH);

        var result = service.updateMetadata(UpdateRoomMetadataCommand.of(ID, name, description));

        verify(roomSavePort).save(room);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.description()).isEqualTo(description);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    private void givenRoomFound(Operation operation) {
        operation.givenRoom(roomFindPort, Optional.of(room));
    }

    private static Room activeRoom() {
        return Room.of(ID, CODE, NAME, DESCRIPTION, CREATED_AT);
    }

    private static Room renamedRoom(Instant lastUpdateAt) {
        var renamed = activeRoom();
        renamed.updateName(NEW_NAME, lastUpdateAt);
        return renamed;
    }

    private static Room deletedRoom(Instant deletedAt) {
        var deleted = activeRoom();
        deleted.delete(deletedAt);
        return deleted;
    }

    /**
     * One change operation: the lookup it reads the target room through, a room whose last change
     * is at the given instant, and the call that runs the operation. Restore reads the deleted-room
     * lookup, so an active room is not its target and a lookup failure means there is nothing to
     * restore.
     */
    private record Operation(
            String name,
            Function<RoomFindPort, Optional<Room>> lookup,
            Function<Instant, Room> roomWithLastUpdate,
            Function<UpdateRoomService, UpdateRoomResult> apply) {

        void givenRoom(RoomFindPort port, Optional<Room> found) {
            when(lookup.apply(port)).thenReturn(found);
        }
    }

    private static final Operation UPDATE_CODE =
            new Operation(
                    "updateCode",
                    port -> port.findNotDeletedById(ROOM_ID),
                    UpdateRoomServiceTest::renamedRoom,
                    target -> target.updateCode(UpdateRoomCodeCommand.of(ID, NEW_CODE)));

    private static final Operation UPDATE_METADATA =
            new Operation(
                    "updateMetadata",
                    port -> port.findNotDeletedById(ROOM_ID),
                    UpdateRoomServiceTest::renamedRoom,
                    target ->
                            target.updateMetadata(
                                    UpdateRoomMetadataCommand.of(ID, NEW_NAME, NEW_DESCRIPTION)));

    private static final Operation DELETE =
            new Operation(
                    "delete",
                    port -> port.findNotDeletedById(ROOM_ID),
                    UpdateRoomServiceTest::renamedRoom,
                    target -> target.delete(DeleteRoomCommand.of(ID)));

    private static final Operation RESTORE =
            new Operation(
                    "restore",
                    port -> port.findDeletedById(ROOM_ID),
                    UpdateRoomServiceTest::deletedRoom,
                    target -> target.restore(RestoreRoomCommand.of(ID)));

    private static Stream<Arguments> operations() {
        return Stream.of(
                arguments(UPDATE_CODE.name(), UPDATE_CODE),
                arguments(UPDATE_METADATA.name(), UPDATE_METADATA),
                arguments(DELETE.name(), DELETE),
                arguments(RESTORE.name(), RESTORE));
    }

    private static Stream<Arguments> invalidCodes() {
        return Stream.of(
                arguments(null, NullPointerException.class),
                arguments("", IllegalValueConditionException.class),
                arguments(" ", IllegalValueConditionException.class),
                arguments(
                        "a".repeat(Room.MAX_CODE_LENGTH + 1),
                        IllegalComparisonConditionException.class));
    }

    private static Stream<Arguments> invalidMetadata() {
        return Stream.of(
                arguments(null, DESCRIPTION, NullPointerException.class),
                arguments("", DESCRIPTION, IllegalValueConditionException.class),
                arguments(" ", DESCRIPTION, IllegalValueConditionException.class),
                arguments(
                        "a".repeat(Room.MAX_NAME_LENGTH + 1),
                        DESCRIPTION,
                        IllegalComparisonConditionException.class),
                arguments(NEW_NAME, null, NullPointerException.class),
                arguments(
                        NEW_NAME,
                        "a".repeat(Room.MAX_DESCRIPTION_LENGTH + 1),
                        IllegalComparisonConditionException.class));
    }
}
