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
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("좌석 변경 유스케이스")
class UpdateSeatServiceTest {
    private static final UUID SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final SeatId SEAT = SeatId.of(SEAT_ID);
    private static final RoomId ROOM = RoomId.of(ROOM_ID);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED_AT.plusSeconds(60);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String CODE = "A-01";
    private static final String NEW_CODE = "A-02";

    @Mock private SeatFindPort seatFindPort;
    @Mock private SeatSavePort seatSavePort;

    private UpdateSeatService service;
    private Seat seat;

    @BeforeEach
    void setUp() {
        service = new UpdateSeatService(seatSavePort, seatFindPort, CLOCK);
        seat = activeSeat();
    }

    @Test
    @DisplayName("코드를 변경하여 저장하고 현재 시각이 반영된 결과를 반환한다")
    void updatesCodeAndReturnsSavedSeat() {
        givenSeatFound(UPDATE_CODE);

        var result = service.updateCode(UpdateSeatCodeCommand.of(SEAT_ID, NEW_CODE));

        verify(seatSavePort).save(seat);
        assertThat(seat.getCode()).isEqualTo(NEW_CODE);
        assertThat(seat.getUpdatedAt()).isEqualTo(NOW);
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(result)
                .isEqualTo(new UpdateSeatResult(SEAT, ROOM, NEW_CODE, CREATED_AT, NOW, null));
    }

    @Test
    @DisplayName("좌석을 삭제하여 저장하고 삭제 시각이 반영된 결과를 반환한다")
    void deletesSeatAndReturnsSavedSeat() {
        givenSeatFound(DELETE);

        var result = service.delete(DeleteSeatCommand.of(SEAT_ID));

        verify(seatSavePort).save(seat);
        assertThat(seat.isDeleted()).isTrue();
        assertThat(seat.getDeletedAt()).isEqualTo(NOW);
        assertThat(seat.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result).isEqualTo(new UpdateSeatResult(SEAT, ROOM, CODE, CREATED_AT, NOW, NOW));
    }

    @Test
    @DisplayName("삭제된 좌석을 복구하여 저장하고 복원 시각이 반영된 결과를 반환한다")
    void restoresDeletedSeatAndReturnsSavedSeat() {
        seat = deletedSeat(NOW.minusSeconds(30));
        givenSeatFound(RESTORE);

        var result = service.restore(RestoreSeatCommand.of(SEAT_ID));

        verify(seatSavePort).save(seat);
        assertThat(seat.isDeleted()).isFalse();
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isEqualTo(NOW);
        assertThat(result).isEqualTo(new UpdateSeatResult(SEAT, ROOM, CODE, CREATED_AT, NOW, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("대상 좌석이 없으면 조회 실패 예외를 전파하고 저장하지 않는다")
    void rejectsMissingSeat(String name, Operation operation) {
        operation.givenSeat(seatFindPort, Optional.empty());

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(seatSavePort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("좌석 조회가 실패하면 해당 예외를 전파하고 저장하지 않는다")
    void propagatesFindFailure(String name, Operation operation) {
        var failure = new IllegalStateException("Seat lookup failed");
        when(operation.lookup().apply(seatFindPort)).thenThrow(failure);

        assertThatThrownBy(() -> operation.apply().apply(service)).isSameAs(failure);

        verifyNoInteractions(seatSavePort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("좌석 저장이 실패하면 변경 결과 대신 저장 예외를 전파한다")
    void propagatesSaveFailure(String name, Operation operation) {
        seat = operation.seatWithLastUpdate().apply(NOW);
        operation.givenSeat(seatFindPort, Optional.of(seat));
        var failure = new IllegalStateException("Seat storage failed");
        doThrow(failure).when(seatSavePort).save(seat);

        assertThatThrownBy(() -> operation.apply().apply(service)).isSameAs(failure);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("현재 시각이 마지막 변경보다 이르면 도메인 예외를 전파하고 저장하지 않는다")
    void rejectsTimeBeforeLastUpdate(String name, Operation operation) {
        seat = operation.seatWithLastUpdate().apply(NOW.plusSeconds(1));
        operation.givenSeat(seatFindPort, Optional.of(seat));

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(seatSavePort);
    }

    @ParameterizedTest
    @MethodSource("invalidCodes")
    @DisplayName("도메인이 코드를 거부하면 예외를 전파하고 저장하지 않는다")
    void rejectsInvalidCode(String code, Class<? extends Throwable> exceptionType) {
        givenSeatFound(UPDATE_CODE);

        assertThatThrownBy(() -> service.updateCode(UpdateSeatCodeCommand.of(SEAT_ID, code)))
                .isInstanceOf(exceptionType);

        verifyNoInteractions(seatSavePort);
        assertThat(seat.getCode()).isEqualTo(CODE);
    }

    @Test
    @DisplayName("삭제된 좌석의 코드 변경은 삭제 상태 예외를 전파하고 저장하지 않는다")
    void rejectsCodeUpdateOnDeletedSeat() {
        seat = deletedSeat(NOW.minusSeconds(30));
        givenSeatFound(UPDATE_CODE);

        assertThatThrownBy(() -> service.updateCode(UpdateSeatCodeCommand.of(SEAT_ID, NEW_CODE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");

        verifyNoInteractions(seatSavePort);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    @DisplayName("마지막 변경 시각과 현재 시각이 같으면 변경을 저장한다")
    void acceptsTimeAtLastUpdate(String name, Operation operation) {
        seat = operation.seatWithLastUpdate().apply(NOW);
        operation.givenSeat(seatFindPort, Optional.of(seat));

        var result = operation.apply().apply(service);

        verify(seatSavePort).save(seat);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대 길이의 코드로 변경할 수 있다")
    void acceptsCodeAtMaxLengthOnUpdate() {
        givenSeatFound(UPDATE_CODE);
        var code = "a".repeat(Seat.MAX_CODE_LENGTH);

        var result = service.updateCode(UpdateSeatCodeCommand.of(SEAT_ID, code));

        verify(seatSavePort).save(seat);
        assertThat(result.code()).isEqualTo(code);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeOperations")
    @DisplayName("생성 시각과 현재 시각이 같으면 변경을 저장한다")
    void acceptsTimeAtCreation(String name, Operation operation) {
        service =
                new UpdateSeatService(
                        seatSavePort, seatFindPort, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
        seat = activeSeat();
        operation.givenSeat(seatFindPort, Optional.of(seat));

        var result = operation.apply().apply(service);

        verify(seatSavePort).save(seat);
        assertThat(result.updatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeOperations")
    @DisplayName("현재 시각이 생성 시각보다 이르면 저장하지 않는다")
    void rejectsTimeBeforeCreation(String name, Operation operation) {
        service =
                new UpdateSeatService(
                        seatSavePort,
                        seatFindPort,
                        Clock.fixed(CREATED_AT.minusNanos(1), ZoneOffset.UTC));
        operation.givenSeat(seatFindPort, Optional.of(seat));

        assertThatThrownBy(() -> operation.apply().apply(service))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(seatSavePort);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    private static Stream<Arguments> activeOperations() {
        return Stream.of(
                arguments(UPDATE_CODE.name(), UPDATE_CODE), arguments(DELETE.name(), DELETE));
    }

    private void givenSeatFound(Operation operation) {
        operation.givenSeat(seatFindPort, Optional.of(seat));
    }

    private static Seat activeSeat() {
        return Seat.of(SEAT, ROOM, CODE, CREATED_AT);
    }

    private static Seat updatedSeat(Instant lastUpdateAt) {
        var updated = activeSeat();
        updated.updateCode(NEW_CODE, lastUpdateAt);
        return updated;
    }

    private static Seat deletedSeat(Instant deletedAt) {
        var deleted = activeSeat();
        deleted.delete(deletedAt);
        return deleted;
    }

    /**
     * One change operation: the lookup it reads the target seat through, a seat whose last change
     * is at the given instant, and the call that runs the operation. Restore reads the deleted-seat
     * lookup, so an active seat is not its target and a lookup failure means there is nothing to
     * restore.
     */
    private record Operation(
            String name,
            Function<SeatFindPort, Optional<Seat>> lookup,
            Function<Instant, Seat> seatWithLastUpdate,
            Function<UpdateSeatService, UpdateSeatResult> apply) {

        void givenSeat(SeatFindPort port, Optional<Seat> found) {
            when(lookup.apply(port)).thenReturn(found);
        }
    }

    private static final Operation UPDATE_CODE =
            new Operation(
                    "updateCode",
                    port -> port.findNotDeletedById(SEAT),
                    UpdateSeatServiceTest::updatedSeat,
                    target -> target.updateCode(UpdateSeatCodeCommand.of(SEAT_ID, NEW_CODE)));

    private static final Operation DELETE =
            new Operation(
                    "delete",
                    port -> port.findNotDeletedById(SEAT),
                    UpdateSeatServiceTest::updatedSeat,
                    target -> target.delete(DeleteSeatCommand.of(SEAT_ID)));

    private static final Operation RESTORE =
            new Operation(
                    "restore",
                    port -> port.findDeletedById(SEAT),
                    UpdateSeatServiceTest::deletedSeat,
                    target -> target.restore(RestoreSeatCommand.of(SEAT_ID)));

    private static Stream<Arguments> operations() {
        return Stream.of(
                arguments(UPDATE_CODE.name(), UPDATE_CODE),
                arguments(DELETE.name(), DELETE),
                arguments(RESTORE.name(), RESTORE));
    }

    private static Stream<Arguments> invalidCodes() {
        return Stream.of(
                arguments(null, NullPointerException.class),
                arguments("", IllegalValueConditionException.class),
                arguments(" ", IllegalValueConditionException.class),
                arguments(
                        "a".repeat(Seat.MAX_CODE_LENGTH + 1),
                        IllegalComparisonConditionException.class));
    }
}
