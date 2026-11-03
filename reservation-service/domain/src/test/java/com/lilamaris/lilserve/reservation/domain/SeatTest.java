package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.*;

import com.lilamaris.lilserve.kernel.condition.ComparisonCondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("좌석")
class SeatTest {
    private static final String CODE = "A-01";
    private static final SeatId SEAT_ID =
            SeatId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final RoomId ROOM_ID =
            RoomId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant LATER_AT = Instant.parse("2026-01-03T00:00:00Z");
    private static final Instant MID_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant EARLIER_AT = Instant.parse("2025-12-31T00:00:00Z");

    @Test
    @DisplayName("좌석을 생성하면 주값을 보관하고 갱신과 삭제 시점을 비운다")
    void retainsGivenValues() {
        var id = SeatId.of(UUID.randomUUID());
        var roomId = RoomId.of(UUID.randomUUID());

        var seat = Seat.of(id, roomId, CODE, CREATED_AT);

        assertThat(seat.getId()).isEqualTo(id);
        assertThat(seat.getRoomId()).isEqualTo(roomId);
        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(seat.getUpdatedAt()).isNull();
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("코드 길이가 상한과 같으면 좌석을 생성할 수 있다")
    void acceptsCodeAtMaxLength() {
        var seat = Seat.of(SEAT_ID, ROOM_ID, text(20), CREATED_AT);

        assertThat(seat.getCode()).hasSize(20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("코드가 빈 문자열이면 좌석을 생성할 수 없다")
    void rejectsBlankCode(String code) {
        assertThatThrownBy(() -> Seat.of(SEAT_ID, ROOM_ID, code, CREATED_AT))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");
    }

    @Test
    @DisplayName("코드 길이가 상한을 넘으면 좌석을 생성할 수 없다")
    void rejectsCodeAboveMaxLength() {
        String code = text(21);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Seat.of(SEAT_ID, ROOM_ID, code, CREATED_AT));

        assertThat(exception).hasMessage("must be at most 20.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(code.length());
        assertThat(exception.references()).isEqualTo(List.of(20));
    }

    @Test
    @DisplayName("식별자가 null이면 좌석을 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(() -> Seat.of(null, ROOM_ID, CODE, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("룸 식별자가 null이면 좌석을 생성할 수 없다")
    void rejectsNullRoomId() {
        assertThatThrownBy(() -> Seat.of(SEAT_ID, null, CODE, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("코드가 null이면 좌석을 생성할 수 없다")
    void rejectsNullCode() {
        assertThatThrownBy(() -> Seat.of(SEAT_ID, ROOM_ID, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("생성 시점이 null이면 좌석을 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> Seat.of(SEAT_ID, ROOM_ID, CODE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("삭제하면 삭제 시점이 스키마 기준 시점 이후로 남는다")
    void keepsDeletedAtAtOrAfterSchemaBaseline() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);

        seat.delete(LATER_AT);

        assertThat(seat.getDeletedAt())
                .isAfterOrEqualTo(coalesce(seat.getUpdatedAt(), seat.getCreatedAt()));
    }

    @Test
    @DisplayName("코드를 갱신하면 새 코드와 갱신 시점을 보관한다")
    void updatesCodeAndTimestamp() {
        var seat = seat();

        seat.updateCode("A-02", UPDATED_AT);

        assertThat(seat.getCode()).isEqualTo("A-02");
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(seat.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("같은 코드로 갱신하면 갱신 시점만 이동한다")
    void movesTimestampWhenCodeIsUnchanged() {
        var seat = seat();

        seat.updateCode(CODE, UPDATED_AT);

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("코드 길이가 상한과 같으면 코드를 갱신할 수 있다")
    void acceptsCodeAtMaxLengthOnUpdate() {
        var seat = seat();
        String code = text(20);

        seat.updateCode(code, UPDATED_AT);

        assertThat(seat.getCode()).isEqualTo(code);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("코드가 빈 문자열이면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsBlankCodeOnUpdate(String code) {
        var seat = seat();

        assertThatThrownBy(() -> seat.updateCode(code, UPDATED_AT))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드가 null이면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsNullCodeOnUpdate() {
        var seat = seat();

        assertThatThrownBy(() -> seat.updateCode(null, UPDATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 길이가 상한을 넘으면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsCodeAboveMaxLengthOnUpdate() {
        var seat = seat();

        assertThatThrownBy(() -> seat.updateCode(text(21), UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 20.");

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 갱신 시점이 생성 시점보다 앞이면 코드가 그대로 남는다")
    void rejectsCodeUpdateBeforeCreatedAt() {
        var seat = seat();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> seat.updateCode("A-02", EARLIER_AT));

        assertThat(exception).hasMessage("must be after or equal to " + CREATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(CREATED_AT));
        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 갱신 시점이 마지막 갱신보다 앞이면 거부한다")
    void rejectsCodeUpdateBeforeLastUpdate() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> seat.updateCode("A-02", MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + UPDATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(UPDATED_AT));
        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("마지막 갱신과 같은 시점으로 코드를 갱신할 수 있다")
    void acceptsCodeUpdateAtLastUpdateInstant() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);

        seat.updateCode("A-02", UPDATED_AT);

        assertThat(seat.getCode()).isEqualTo("A-02");
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("코드 갱신 시점이 null이면 코드와 갱신 시점이 그대로 남는다")
    void rejectsNullUpdatedAtOnCodeUpdate() {
        var seat = seat();

        assertThatThrownBy(() -> seat.updateCode("A-02", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제된 좌석의 코드 갱신은 삭제 상태를 먼저 거부하고 상태를 그대로 둔다")
    void rejectsCodeUpdateOnDeletedSeat() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        assertThatThrownBy(() -> seat.updateCode(text(21), LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");

        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(seat.getDeletedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제하면 삭제 시점을 보관하고 갱신 시점을 삭제 시점으로 옮긴다")
    void marksDeletedAtAndTimestamp() {
        var seat = seat();

        seat.delete(LATER_AT);

        assertThat(seat.getDeletedAt()).isEqualTo(LATER_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(seat.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("마지막 갱신과 같은 시점에 좌석을 삭제할 수 있다")
    void acceptsDeleteAtLastUpdateInstant() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);

        seat.delete(UPDATED_AT);

        assertThat(seat.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("갱신되지 않은 좌석은 생성 시점과 같은 시점에 삭제할 수 있다")
    void deletesSeatAtCreatedAtWhenNeverUpdated() {
        var seat = seat();

        seat.delete(CREATED_AT);

        assertThat(seat.getDeletedAt()).isEqualTo(CREATED_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(seat.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("이미 삭제된 좌석을 다시 삭제하면 거부한다")
    void rejectsSecondDelete() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        assertThatThrownBy(() -> seat.delete(LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");
    }

    @Test
    @DisplayName("삭제 시점이 생성 시점보다 앞이면 거부한다")
    void rejectsDeleteBeforeCreatedAt() {
        var seat = seat();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> seat.delete(EARLIER_AT));

        assertThat(exception).hasMessage("must be after or equal to " + CREATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(CREATED_AT));
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시점이 마지막 갱신보다 앞이면 거부한다")
    void rejectsDeleteBeforeLastUpdate() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> seat.delete(MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + UPDATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(UPDATED_AT));
        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제 시점이 null이면 거부한다")
    void rejectsNullDeletedAt() {
        var seat = seat();

        assertThatThrownBy(() -> seat.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("복원하면 삭제 시점을 비우고 갱신 시점을 복원 시점으로 옮긴다")
    void clearsDeletedAtAndTimestampOnRestore() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        seat.restore(LATER_AT);

        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(seat.isDeleted()).isFalse();
        assertThat(seat.getCode()).isEqualTo(CODE);
        assertThat(seat.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제 시점과 같은 시점에 좌석을 복원할 수 있다")
    void acceptsRestoreAtDeletedInstant() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        seat.restore(UPDATED_AT);

        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("복원 시점이 마지막 갱신보다 앞이면 거부하고 삭제 상태를 유지한다")
    void rejectsRestoreBeforeLastUpdate() {
        var seat = seat();
        seat.updateCode(CODE, UPDATED_AT);
        seat.delete(LATER_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> seat.restore(MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + LATER_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(LATER_AT));
        assertThat(seat.getDeletedAt()).isEqualTo(LATER_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("복원 시점이 null이면 거부하고 삭제 상태를 유지한다")
    void rejectsNullRestoredAt() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        assertThatThrownBy(() -> seat.restore(null)).isInstanceOf(NullPointerException.class);

        assertThat(seat.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 좌석은 복원을 거부하고 상태를 그대로 둔다")
    void rejectsRestoreOnNotDeletedSeat() {
        var seat = seat();

        assertThatThrownBy(() -> seat.restore(UPDATED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");

        assertThat(seat.getDeletedAt()).isNull();
        assertThat(seat.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("이미 복원된 좌석을 다시 복원하면 거부한다")
    void rejectsSecondRestore() {
        var seat = seat();
        seat.delete(UPDATED_AT);
        seat.restore(LATER_AT);

        assertThatThrownBy(() -> seat.restore(LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");

        assertThat(seat.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("복원된 좌석은 다시 삭제할 수 있다")
    void deletesRestoredSeat() {
        var seat = seat();
        seat.delete(UPDATED_AT);
        seat.restore(LATER_AT);

        seat.delete(LATER_AT);

        assertThat(seat.getDeletedAt()).isEqualTo(LATER_AT);
        assertThat(seat.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(seat.getCode()).isEqualTo(CODE);
    }

    @Test
    @DisplayName("삭제되지 않은 좌석은 검사를 통과한다")
    void passesValidationWhenNotDeleted() {
        var seat = seat();

        assertThatCode(() -> seat.validateNotDeleted()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제된 좌석은 검사를 거부한다")
    void rejectsValidationWhenDeleted() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        assertThatThrownBy(() -> seat.validateNotDeleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");
    }

    @Test
    @DisplayName("삭제된 좌석은 삭제 검사를 통과한다")
    void passesDeletedValidation() {
        var seat = seat();
        seat.delete(UPDATED_AT);

        assertThatCode(() -> seat.validateDeleted()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제되지 않은 좌석은 삭제 검사를 거부한다")
    void rejectsDeletedValidationWhenNotDeleted() {
        var seat = seat();

        assertThatThrownBy(() -> seat.validateDeleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");
    }

    @Test
    @DisplayName("생성 시점과 같은 시점으로 코드를 갱신할 수 있다")
    void acceptsCodeUpdateAtCreatedAt() {
        var seat = seat();

        seat.updateCode("NEW-CODE", CREATED_AT);

        assertThat(seat.getCode()).isEqualTo("NEW-CODE");
        assertThat(seat.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(seat.getDeletedAt()).isNull();
    }

    private static Seat seat() {
        return Seat.of(SEAT_ID, ROOM_ID, CODE, CREATED_AT);
    }

    private static Instant coalesce(Instant updatedAt, Instant createdAt) {
        return Objects.requireNonNullElse(updatedAt, createdAt);
    }

    private static String text(int length) {
        return "a".repeat(length);
    }
}
