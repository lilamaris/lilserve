package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies blackout value constraints, lifecycle transitions, and preservation of state on invalid
 * changes.
 */
@DisplayName("스케줄 일시 사용 불가")
class BlackoutScheduleTest {
    private static final BlackoutScheduleId ID = BlackoutScheduleId.of(UUID.randomUUID());
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    private static final ScheduleId SCHEDULE_ID = ScheduleId.of(UUID.randomUUID());
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant STARTS_AT = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plusSeconds(3600);

    @Test
    @DisplayName("생성 시 방과 스케줄 및 차단 정보를 보관한다")
    void retainsBlackoutDetails() {
        var blackout = blackout();

        assertThat(blackout.getId()).isEqualTo(ID);
        assertThat(blackout.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(blackout.getScheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(blackout.getReason()).isEqualTo("Maintenance");
        assertThat(blackout.getStartsAt()).isEqualTo(STARTS_AT);
        assertThat(blackout.getEndsAt()).isEqualTo(ENDS_AT);
        assertThat(blackout.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(blackout.getUpdatedAt()).isNull();
        assertThat(blackout.getDeletedAt()).isNull();
        assertThat(blackout.isDeleted()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1000})
    @DisplayName("스키마가 허용하는 빈 사유와 최대 길이 사유를 저장한다")
    void acceptsReasonWithinColumnLength(int length) {
        var reason = "a".repeat(length);
        assertThat(create(reason, STARTS_AT, ENDS_AT).getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("최대 길이를 초과하는 사유는 생성할 수 없다")
    void rejectsTooLongReason() {
        assertThatThrownBy(() -> create("a".repeat(1001), STARTS_AT, ENDS_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("종료가 시작과 같거나 이르면 생성할 수 없다")
    void rejectsInvalidPeriod(int seconds) {
        assertThatThrownBy(() -> create("Maintenance", STARTS_AT, STARTS_AT.plusSeconds(seconds)))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"id", "roomId", "scheduleId", "reason", "startsAt", "endsAt", "createdAt"})
    @DisplayName("필수 속성이 null이면 생성할 수 없다")
    void rejectsNullRequiredFields(String field) {
        assertThatThrownBy(
                        () ->
                                BlackoutSchedule.of(
                                        field.equals("id") ? null : ID,
                                        field.equals("roomId") ? null : ROOM_ID,
                                        field.equals("scheduleId") ? null : SCHEDULE_ID,
                                        field.equals("reason") ? null : "Maintenance",
                                        field.equals("startsAt") ? null : STARTS_AT,
                                        field.equals("endsAt") ? null : ENDS_AT,
                                        field.equals("createdAt") ? null : CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("사유 수정 시 새로운 사유와 수정 시각을 보관한다")
    void updatesReason() {
        var blackout = blackout();
        blackout.updateReason("Inspection", CREATED_AT);

        assertThat(blackout.getReason()).isEqualTo("Inspection");
        assertThat(blackout.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("기간 수정 시 새로운 구간과 수정 시각을 보관한다")
    void updatesPeriod() {
        var blackout = blackout();
        var updatedAt = CREATED_AT.plusSeconds(1);
        blackout.updatePeriod(STARTS_AT.plusSeconds(1), ENDS_AT.plusSeconds(1), updatedAt);

        assertThat(blackout.getStartsAt()).isEqualTo(STARTS_AT.plusSeconds(1));
        assertThat(blackout.getEndsAt()).isEqualTo(ENDS_AT.plusSeconds(1));
        assertThat(blackout.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("잘못된 기간 수정은 기존 기간과 수정 시각을 보존한다")
    void preservesStateWhenPeriodIsInvalid() {
        var blackout = blackout();

        assertThatThrownBy(() -> blackout.updatePeriod(ENDS_AT, STARTS_AT, CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getStartsAt()).isEqualTo(STARTS_AT);
        assertThat(blackout.getEndsAt()).isEqualTo(ENDS_AT);
        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("긴 사유로 수정하면 기존 사유와 수정 시각을 보존한다")
    void preservesStateWhenReasonIsInvalid() {
        var blackout = blackout();

        assertThatThrownBy(() -> blackout.updateReason("a".repeat(1001), CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getReason()).isEqualTo("Maintenance");
        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("생성보다 이른 수정은 사유와 수정 시각을 변경하지 않는다")
    void rejectsUpdateBeforeCreation() {
        var blackout = blackout();

        assertThatThrownBy(() -> blackout.updateReason("Inspection", CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getReason()).isEqualTo("Maintenance");
        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("마지막 수정보다 이른 수정은 기간과 수정 시각을 변경하지 않는다")
    void rejectsUpdateBeforeLastUpdate() {
        var blackout = blackout();
        var updatedAt = CREATED_AT.plusSeconds(2);
        blackout.updateReason("Inspection", updatedAt);

        assertThatThrownBy(
                        () ->
                                blackout.updatePeriod(
                                        STARTS_AT.plusSeconds(1),
                                        ENDS_AT,
                                        CREATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getStartsAt()).isEqualTo(STARTS_AT);
        assertThat(blackout.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("삭제 시 삭제 시각과 수정 시각을 함께 기록한다")
    void deletesBlackout() {
        var blackout = blackout();
        var deletedAt = CREATED_AT.plusSeconds(1);

        blackout.delete(deletedAt);

        assertThat(blackout.isDeleted()).isTrue();
        assertThat(blackout.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(blackout.getUpdatedAt()).isEqualTo(deletedAt);
        blackout.validateDeleted();
    }

    @Test
    @DisplayName("생성 시각과 같은 시각에 삭제할 수 있다")
    void deletesAtCreationTime() {
        var blackout = blackout();
        blackout.delete(CREATED_AT);
        assertThat(blackout.getDeletedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("생성보다 이른 삭제는 상태를 변경하지 않는다")
    void rejectsDeletionBeforeCreation() {
        var blackout = blackout();

        assertThatThrownBy(() -> blackout.delete(CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getDeletedAt()).isNull();
        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("마지막 수정보다 이른 삭제는 상태를 변경하지 않는다")
    void rejectsDeletionBeforeLastUpdate() {
        var blackout = blackout();
        var updatedAt = CREATED_AT.plusSeconds(2);
        blackout.updateReason("Inspection", updatedAt);

        assertThatThrownBy(() -> blackout.delete(CREATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getDeletedAt()).isNull();
        assertThat(blackout.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("삭제를 반복하면 최초 삭제 시각을 보존하고 거부한다")
    void rejectsRepeatedDeletion() {
        var blackout = blackout();
        blackout.delete(CREATED_AT);

        assertThatThrownBy(() -> blackout.delete(CREATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(blackout.getDeletedAt()).isEqualTo(CREATED_AT);
        assertThat(blackout.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reason", "period"})
    @DisplayName("삭제된 차단은 사유나 기간을 수정할 수 없다")
    void rejectsChangesAfterDeletion(String field) {
        var blackout = blackout();
        blackout.delete(CREATED_AT);

        assertThatThrownBy(
                        () -> {
                            if (field.equals("reason")) {
                                blackout.updateReason("Inspection", CREATED_AT.plusSeconds(1));
                            } else {
                                blackout.updatePeriod(
                                        STARTS_AT.plusSeconds(1),
                                        ENDS_AT,
                                        CREATED_AT.plusSeconds(1));
                            }
                        })
                .isInstanceOf(IllegalStateException.class);

        assertThat(blackout.getReason()).isEqualTo("Maintenance");
        assertThat(blackout.getStartsAt()).isEqualTo(STARTS_AT);
        assertThat(blackout.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("복원 시 삭제 시각을 지우고 수정 시각을 기록한다")
    void restoresDeletedBlackout() {
        var blackout = blackout();
        blackout.delete(CREATED_AT);
        var restoredAt = CREATED_AT.plusSeconds(1);

        blackout.restore(restoredAt);

        assertThat(blackout.isDeleted()).isFalse();
        assertThat(blackout.getDeletedAt()).isNull();
        assertThat(blackout.getUpdatedAt()).isEqualTo(restoredAt);
        blackout.validateNotDeleted();
        blackout.updateReason("Inspection", restoredAt);
        assertThat(blackout.getReason()).isEqualTo("Inspection");
    }

    @Test
    @DisplayName("삭제되지 않은 차단은 복원할 수 없다")
    void rejectsRestorationOfActiveBlackout() {
        var blackout = blackout();

        assertThatThrownBy(() -> blackout.restore(CREATED_AT))
                .isInstanceOf(IllegalStateException.class);

        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제보다 이른 복원은 삭제 상태와 수정 시각을 보존한다")
    void rejectsRestorationBeforeDeletion() {
        var blackout = blackout();
        var deletedAt = CREATED_AT.plusSeconds(2);
        blackout.delete(deletedAt);

        assertThatThrownBy(() -> blackout.restore(CREATED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(blackout.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(blackout.getUpdatedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("삭제 시각이 null이면 상태를 변경하지 않는다")
    void rejectsNullDeletionTime() {
        var blackout = blackout();
        assertThatThrownBy(() -> blackout.delete(null)).isInstanceOf(NullPointerException.class);
        assertThat(blackout.getDeletedAt()).isNull();
        assertThat(blackout.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("복원 시각이 null이면 삭제 상태를 보존한다")
    void rejectsNullRestorationTime() {
        var blackout = blackout();
        blackout.delete(CREATED_AT);
        assertThatThrownBy(() -> blackout.restore(null)).isInstanceOf(NullPointerException.class);
        assertThat(blackout.getDeletedAt()).isEqualTo(CREATED_AT);
        assertThat(blackout.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    private BlackoutSchedule blackout() {
        return create("Maintenance", STARTS_AT, ENDS_AT);
    }

    private BlackoutSchedule create(String reason, Instant startsAt, Instant endsAt) {
        return BlackoutSchedule.of(ID, ROOM_ID, SCHEDULE_ID, reason, startsAt, endsAt, CREATED_AT);
    }
}
