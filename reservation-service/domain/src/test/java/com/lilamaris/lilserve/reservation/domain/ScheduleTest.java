package com.lilamaris.lilserve.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilserve.kernel.condition.ComparisonCondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("스케줄")
class ScheduleTest {
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final LocalTime START_AT = LocalTime.of(10, 0);
    private static final LocalTime END_AT = LocalTime.of(12, 0);

    @Test
    @DisplayName("스케줄은 입력한 값을 그대로 유지한다")
    void retainsSuppliedValues() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        assertThat(schedule.getId().getValue()).isEqualTo(SCHEDULE_ID);
        assertThat(schedule.getRoomId().getValue()).isEqualTo(ROOM_ID);
        assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.getStartAt()).isEqualTo(START_AT);
        assertThat(schedule.getEndAt()).isEqualTo(END_AT);
        assertThat(schedule.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(schedule.getDeletedAt()).isNull();
        assertThat(schedule.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("종료 시각이 시작 시각과 같으면 스케줄을 생성할 수 없다")
    void rejectsEndAtEqualToStartAt() {
        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        START_AT,
                                        START_AT,
                                        CREATED_AT));

        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER);
        assertThat(exception.subject()).isEqualTo(START_AT);
        assertThat(exception.references()).isEqualTo(List.of(START_AT));
    }

    @Test
    @DisplayName("종료 시각이 시작 시각보다 앞이면 스케줄을 생성할 수 없다")
    void rejectsEndAtBeforeStartAt() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        END_AT,
                                        START_AT,
                                        CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("식별자가 null이면 스케줄을 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        null,
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        START_AT,
                                        END_AT,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("룸 식별자가 null이면 스케줄을 생성할 수 없다")
    void rejectsNullRoomId() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        null,
                                        DayOfWeek.MONDAY,
                                        START_AT,
                                        END_AT,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("요일이 null이면 스케줄을 생성할 수 없다")
    void rejectsNullDayOfWeek() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        null,
                                        START_AT,
                                        END_AT,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("시작 시각이 null이면 스케줄을 생성할 수 없다")
    void rejectsNullStartAt() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        null,
                                        END_AT,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("종료 시각이 null이면 스케줄을 생성할 수 없다")
    void rejectsNullEndAt() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        START_AT,
                                        null,
                                        CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("생성 시점이 null이면 스케줄을 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(
                        () ->
                                Schedule.of(
                                        ScheduleId.of(SCHEDULE_ID),
                                        RoomId.of(ROOM_ID),
                                        DayOfWeek.MONDAY,
                                        START_AT,
                                        END_AT,
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("삭제 시점 유지하면 삭제된 스케줄이 된다")
    void marksDeletedAtDeletion() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        schedule.delete(DELETED_AT);

        assertThat(schedule.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(schedule.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제 시점이 생성 시점과 같으면 삭제할 수 있다")
    void acceptsDeletionAtCreatedAt() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        schedule.delete(CREATED_AT);

        assertThat(schedule.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제 시점이 생성 시점보다 앞이면 삭제할 수 없다")
    void rejectsDeletionBeforeCreatedAt() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);
        Instant beforeCreatedAt = CREATED_AT.minusSeconds(1);

        assertThatThrownBy(() -> schedule.delete(beforeCreatedAt))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(schedule.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제 시점이 null이면 삭제할 수 없다")
    void rejectsNullDeletedAt() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        assertThatThrownBy(() -> schedule.delete(null)).isInstanceOf(NullPointerException.class);

        assertThat(schedule.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 삭제된 스케줄은 다시 삭제할 수 없다")
    void rejectsSecondDeletion() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);
        schedule.delete(DELETED_AT);

        assertThatThrownBy(() -> schedule.delete(DELETED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(schedule.getDeletedAt()).isEqualTo(DELETED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 스케줄은 사용 검증에 통과한다")
    void passesNotDeletedValidationWhenNotDeleted() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        schedule.validateNotDeleted();
        assertThat(schedule.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 스케줄은 사용 검증에 통과하지 못한다")
    void failsNotDeletedValidationWhenDeleted() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);
        schedule.delete(DELETED_AT);

        assertThatThrownBy(schedule::validateNotDeleted).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("삭제된 스케줄은 삭제 검증에 통과한다")
    void passesDeletedValidationWhenDeleted() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);
        schedule.delete(DELETED_AT);

        schedule.validateDeleted();
        assertThat(schedule.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제되지 않은 스케줄은 삭제 검증에 통과하지 못한다")
    void failsDeletedValidationWhenNotDeleted() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.MONDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        assertThatThrownBy(schedule::validateDeleted).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일요일 스케줄은 ISO 요일 7을 유지한다")
    void retainsSundayAsIsoDaySeven() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.SUNDAY,
                        START_AT,
                        END_AT,
                        CREATED_AT);

        assertThat(schedule.getDayOfWeek().getValue()).isEqualTo(7);
    }

    @Test
    @DisplayName("자정 이후 시작하는 스케줄을 생성할 수 있다")
    void acceptsLateNightSlot() {
        var schedule =
                Schedule.of(
                        ScheduleId.of(SCHEDULE_ID),
                        RoomId.of(ROOM_ID),
                        DayOfWeek.FRIDAY,
                        LocalTime.of(23, 0),
                        LocalTime.of(23, 30),
                        CREATED_AT);

        assertThat(schedule.getStartAt()).isEqualTo(LocalTime.of(23, 0));
        assertThat(schedule.getEndAt()).isEqualTo(LocalTime.of(23, 30));
    }
}
