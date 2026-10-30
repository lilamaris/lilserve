package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.*;

import com.lilamaris.lilserve.kernel.condition.ComparisonCondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("룸")
class RoomTest {
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant LATER_AT = Instant.parse("2026-01-03T00:00:00Z");
    private static final Instant MID_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant EARLIER_AT = Instant.parse("2025-12-31T00:00:00Z");

    @Test
    @DisplayName("룸을 생성하면 주값을 보관하고 갱신과 삭제 시점을 비운다")
    void retainsGivenValues() {
        var id = UUID.randomUUID();

        var room = Room.of(id, CODE, NAME, DESCRIPTION, CREATED_AT);

        assertThat(room.getId()).isEqualTo(RoomId.of(id));
        assertThat(room.getId().getValue()).isEqualTo(id);
        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(room.getUpdatedAt()).isNull();
        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("문자열 길이가 상한과 같으면 룸을 생성할 수 있다")
    void acceptsTextsAtMaxLength() {
        var room = Room.of(UUID.randomUUID(), text(20), text(300), text(1000), CREATED_AT);

        assertThat(room.getCode()).hasSize(20);
        assertThat(room.getName()).hasSize(300);
        assertThat(room.getDescription()).hasSize(1000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("코드가 빈 문자열이면 룸을 생성할 수 없다")
    void rejectsBlankCode(String code) {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), code, NAME, DESCRIPTION, CREATED_AT))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("이름이 빈 문자열이면 룸을 생성할 수 없다")
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), CODE, name, DESCRIPTION, CREATED_AT))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("설명이 빈 문자열이어도 룸을 생성할 수 있다")
    void acceptsBlankDescription(String description) {
        var room = Room.of(UUID.randomUUID(), CODE, NAME, description, CREATED_AT);

        assertThat(room.getDescription()).isEqualTo(description);
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("코드 길이가 상한을 넘으면 룸을 생성할 수 없다")
    void rejectsCodeAboveMaxLength() {
        String code = text(21);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Room.of(UUID.randomUUID(), code, NAME, DESCRIPTION, CREATED_AT));

        assertThat(exception).hasMessage("must be at most 20.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(code.length());
        assertThat(exception.references()).isEqualTo(List.of(20));
    }

    @Test
    @DisplayName("이름 길이가 상한을 넘으면 룸을 생성할 수 없다")
    void rejectsNameAboveMaxLength() {
        String name = text(301);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Room.of(UUID.randomUUID(), CODE, name, DESCRIPTION, CREATED_AT));

        assertThat(exception).hasMessage("must be at most 300.");
        assertThat(exception.references()).isEqualTo(List.of(300));
    }

    @Test
    @DisplayName("설명 길이가 상한을 넘으면 룸을 생성할 수 없다")
    void rejectsDescriptionAboveMaxLength() {
        String description = text(1001);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> Room.of(UUID.randomUUID(), CODE, NAME, description, CREATED_AT));

        assertThat(exception).hasMessage("must be at most 1000.");
        assertThat(exception.references()).isEqualTo(List.of(1000));
    }

    @Test
    @DisplayName("식별자가 null이면 룸을 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(() -> Room.of(null, CODE, NAME, DESCRIPTION, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("코드가 null이면 룸을 생성할 수 없다")
    void rejectsNullCode() {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), null, NAME, DESCRIPTION, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("이름이 null이면 룸을 생성할 수 없다")
    void rejectsNullName() {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), CODE, null, DESCRIPTION, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("설명이 null이면 룸을 생성할 수 없다")
    void rejectsNullDescription() {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), CODE, NAME, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("생성 시점이 null이면 룸을 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> Room.of(UUID.randomUUID(), CODE, NAME, DESCRIPTION, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("이름을 갱신하면 새 이름과 갱신 시점을 보관한다")
    void updatesNameAndTimestamp() {
        var room = room();

        room.updateName("renamed", UPDATED_AT);

        assertThat(room.getName()).isEqualTo("renamed");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("이름 길이가 상한을 넘으면 이름 갱신을 거부한다")
    void rejectsNameAboveMaxLengthOnUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateName(text(301), UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 300.");

        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("이름 갱신 시점이 생성 시점보다 앞이면 거부한다")
    void rejectsNameUpdateBeforeCreatedAt() {
        var room = room();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> room.updateName("renamed", EARLIER_AT));

        assertThat(exception).hasMessage("must be after or equal to " + CREATED_AT + ".");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.references()).isEqualTo(List.of(CREATED_AT));
    }

    @Test
    @DisplayName("이름 갱신 시점이 마지막 갱신보다 앞이면 거부한다")
    void rejectsNameUpdateBeforeLastUpdate() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> room.updateName("again", MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + UPDATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(UPDATED_AT));
        assertThat(room.getName()).isEqualTo("renamed");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("마지막 갱신과 같은 시점으로 이름을 갱신할 수 있다")
    void acceptsNameUpdateAtLastUpdateInstant() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);

        room.updateName("again", UPDATED_AT);

        assertThat(room.getName()).isEqualTo("again");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("이름 갱신 시점이 null이면 거부한다")
    void rejectsNullUpdatedAtOnNameUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateName("renamed", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("설명을 갱신하면 새 설명과 갱신 시점을 보관한다")
    void updatesDescriptionAndTimestamp() {
        var room = room();

        room.updateDescription("capacity 200", UPDATED_AT);

        assertThat(room.getDescription()).isEqualTo("capacity 200");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getName()).isEqualTo(NAME);
    }

    @Test
    @DisplayName("설명을 null로 갱신하면 거부하고 설명과 갱신 시점이 그대로 남는다")
    void rejectsNullDescriptionOnUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateDescription(null, UPDATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");

        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("설명이 빈 문자열이어도 설명을 갱신할 수 있다")
    void acceptsBlankDescriptionOnUpdate(String description) {
        var room = room();

        room.updateDescription(description, UPDATED_AT);

        assertThat(room.getDescription()).isEqualTo(description);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("설명 길이가 상한을 넘으면 설명과 갱신 시점이 그대로 남는다")
    void keepsDescriptionAndTimestampWhenLengthRejected() {
        var room = room();

        assertThatThrownBy(() -> room.updateDescription(text(1001), UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 1000.");

        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("설명 갱신 시점이 생성 시점보다 앞이면 거부한다")
    void rejectsDescriptionUpdateBeforeCreatedAt() {
        var room = room();

        assertThatThrownBy(() -> room.updateDescription("capacity 200", EARLIER_AT))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be after or equal to " + CREATED_AT + ".");

        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드를 갱신하면 새 코드와 갱신 시점을 보관한다")
    void updatesCodeAndTimestamp() {
        var room = room();

        room.updateCode("ROOM-02", UPDATED_AT);

        assertThat(room.getCode()).isEqualTo("ROOM-02");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    @DisplayName("같은 코드로 갱신하면 갱신 시점만 이동한다")
    void movesTimestampWhenCodeIsUnchanged() {
        var room = room();

        room.updateCode(CODE, UPDATED_AT);

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("코드 길이가 상한과 같으면 코드를 갱신할 수 있다")
    void acceptsCodeAtMaxLengthOnUpdate() {
        var room = room();
        String code = text(20);

        room.updateCode(code, UPDATED_AT);

        assertThat(room.getCode()).isEqualTo(code);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("코드가 빈 문자열이면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsBlankCodeOnUpdate(String code) {
        var room = room();

        assertThatThrownBy(() -> room.updateCode(code, UPDATED_AT))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드가 null이면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsNullCodeOnUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateCode(null, UPDATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 길이가 상한을 넘으면 코드 갱신을 거부하고 코드와 갱신 시점이 그대로 남는다")
    void rejectsCodeAboveMaxLengthOnUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateCode(text(21), UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 20.");

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 갱신 시점이 생성 시점보다 앞이면 코드가 그대로 남는다")
    void rejectsCodeUpdateBeforeCreatedAt() {
        var room = room();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> room.updateCode("ROOM-02", EARLIER_AT));

        assertThat(exception).hasMessage("must be after or equal to " + CREATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(CREATED_AT));
        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("코드 갱신 시점이 마지막 갱신보다 앞이면 거부한다")
    void rejectsCodeUpdateBeforeLastUpdate() {
        var room = room();
        room.updateCode("ROOM-02", UPDATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> room.updateCode("ROOM-03", MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + UPDATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(UPDATED_AT));
        assertThat(room.getCode()).isEqualTo("ROOM-02");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("마지막 갱신과 같은 시점으로 코드를 갱신할 수 있다")
    void acceptsCodeUpdateAtLastUpdateInstant() {
        var room = room();
        room.updateCode("ROOM-02", UPDATED_AT);

        room.updateCode("ROOM-03", UPDATED_AT);

        assertThat(room.getCode()).isEqualTo("ROOM-03");
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("코드 갱신 시점이 null이면 코드와 갱신 시점이 그대로 남는다")
    void rejectsNullUpdatedAtOnCodeUpdate() {
        var room = room();

        assertThatThrownBy(() -> room.updateCode("ROOM-02", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제된 룸의 코드 갱신은 삭제 상태를 먼저 거부하고 상태를 그대로 둔다")
    void rejectsCodeUpdateOnDeletedRoom() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.updateCode(text(21), LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");

        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getDeletedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제하면 삭제 시점을 보관하고 갱신 시점을 삭제 시점으로 옮긴다")
    void marksDeletedAtAndTimestamp() {
        var room = room();

        room.delete(LATER_AT);

        assertThat(room.getDeletedAt()).isEqualTo(LATER_AT);
        assertThat(room.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(room.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("마지막 갱신과 같은 시점에 룸을 삭제할 수 있다")
    void acceptsDeleteAtLastUpdateInstant() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);

        room.delete(UPDATED_AT);

        assertThat(room.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("갱신되지 않은 룸은 생성 시점과 같은 시점에 삭제할 수 있다")
    void deletesRoomAtCreatedAtWhenNeverUpdated() {
        var room = room();

        room.delete(CREATED_AT);

        assertThat(room.getDeletedAt()).isEqualTo(CREATED_AT);
        assertThat(room.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(room.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제하면 삭제 시점이 스키마 기준 시점 이후로 남는다")
    void keepsDeletedAtAtOrAfterSchemaBaseline() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);

        room.delete(LATER_AT);

        assertThat(room.getDeletedAt())
                .isAfterOrEqualTo(coalesce(room.getUpdatedAt(), room.getCreatedAt()));
    }

    @Test
    @DisplayName("삭제되지 않은 룸은 검사를 통과한다")
    void passesValidationWhenNotDeleted() {
        var room = room();

        assertThatCode(() -> room.validateNotDeleted()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제된 룸은 검사를 거부한다")
    void rejectsValidationWhenDeleted() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.validateNotDeleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");
    }

    @Test
    @DisplayName("삭제된 룸은 삭제 검사를 통과한다")
    void passesDeletedValidation() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatCode(() -> room.validateDeleted()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제되지 않은 룸은 삭제 검사를 거부한다")
    void rejectsDeletedValidationWhenNotDeleted() {
        var room = room();

        assertThatThrownBy(() -> room.validateDeleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");
    }

    @Test
    @DisplayName("삭제된 룸의 이름 갱신은 삭제 상태를 먼저 거부하고 상태를 그대로 둔다")
    void rejectsNameUpdateOnDeletedRoom() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.updateName(text(301), LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");

        assertThat(room.getName()).isEqualTo(NAME);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getDeletedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제된 룸의 설명 갱신은 삭제 상태를 먼저 거부하고 상태를 그대로 둔다")
    void rejectsDescriptionUpdateOnDeletedRoom() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.updateDescription(text(1001), LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");

        assertThat(room.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getDeletedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("이미 삭제된 룸을 다시 삭제하면 거부한다")
    void rejectsSecondDelete() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.delete(LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already deleted.");
    }

    @Test
    @DisplayName("삭제 시점이 생성 시점보다 앞이면 거부한다")
    void rejectsDeleteBeforeCreatedAt() {
        var room = room();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> room.delete(EARLIER_AT));

        assertThat(exception).hasMessage("must be after or equal to " + CREATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(CREATED_AT));
        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제 시점이 마지막 갱신보다 앞이면 거부한다")
    void rejectsDeleteBeforeLastUpdate() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> room.delete(MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + UPDATED_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(UPDATED_AT));
        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제 시점이 null이면 거부한다")
    void rejectsNullDeletedAt() {
        var room = room();

        assertThatThrownBy(() -> room.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("복원하면 삭제 시점을 비우고 갱신 시점을 복원 시점으로 옮긴다")
    void clearsDeletedAtAndTimestampOnRestore() {
        var room = room();
        room.delete(UPDATED_AT);

        room.restore(LATER_AT);

        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(room.isDeleted()).isFalse();
        assertThat(room.getCode()).isEqualTo(CODE);
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제 시점과 같은 시점에 룸을 복원할 수 있다")
    void acceptsRestoreAtDeletedInstant() {
        var room = room();
        room.delete(UPDATED_AT);

        room.restore(UPDATED_AT);

        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("복원 시점이 마지막 갱신보다 앞이면 거부하고 삭제 상태를 유지한다")
    void rejectsRestoreBeforeLastUpdate() {
        var room = room();
        room.updateName("renamed", UPDATED_AT);
        room.delete(LATER_AT);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class, () -> room.restore(MID_AT));

        assertThat(exception).hasMessage("must be after or equal to " + LATER_AT + ".");
        assertThat(exception.references()).isEqualTo(List.of(LATER_AT));
        assertThat(room.getDeletedAt()).isEqualTo(LATER_AT);
        assertThat(room.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("복원 시점이 null이면 거부하고 삭제 상태를 유지한다")
    void rejectsNullRestoredAt() {
        var room = room();
        room.delete(UPDATED_AT);

        assertThatThrownBy(() -> room.restore(null)).isInstanceOf(NullPointerException.class);

        assertThat(room.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(room.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 룸은 복원을 거부하고 상태를 그대로 둔다")
    void rejectsRestoreOnNotDeletedRoom() {
        var room = room();

        assertThatThrownBy(() -> room.restore(UPDATED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");

        assertThat(room.getDeletedAt()).isNull();
        assertThat(room.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("이미 복원된 룸을 다시 복원하면 거부한다")
    void rejectsSecondRestore() {
        var room = room();
        room.delete(UPDATED_AT);
        room.restore(LATER_AT);

        assertThatThrownBy(() -> room.restore(LATER_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already not deleted.");

        assertThat(room.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("복원된 룸은 이후 갱신할 수 있다")
    void updatesRestoredRoom() {
        var room = room();
        room.delete(UPDATED_AT);
        room.restore(LATER_AT);

        room.updateName("renamed", LATER_AT);

        assertThat(room.getName()).isEqualTo("renamed");
        assertThat(room.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(room.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("생성 시점과 같은 시점으로 코드를 갱신할 수 있다")
    void acceptsCodeUpdateAtCreatedAt() {
        var room = room();

        room.updateCode("NEW-CODE", CREATED_AT);

        assertThat(room.getCode()).isEqualTo("NEW-CODE");
        assertThat(room.getUpdatedAt()).isEqualTo(CREATED_AT);
        assertThat(room.getDeletedAt()).isNull();
    }

    private static Room room() {
        return Room.of(UUID.randomUUID(), CODE, NAME, DESCRIPTION, CREATED_AT);
    }

    private static Instant coalesce(Instant updatedAt, Instant createdAt) {
        return Objects.requireNonNullElse(updatedAt, createdAt);
    }

    private static String text(int length) {
        return "a".repeat(length);
    }
}
