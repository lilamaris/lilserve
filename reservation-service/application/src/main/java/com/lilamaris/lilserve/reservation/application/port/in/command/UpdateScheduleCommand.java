package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

/**
 * A request to replace a slot's weekday and local time range. The parent room is inherited from the
 * slot being replaced, so it is not part of the request.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * <p>The time range is carried as a {@link LocalTimeRange}, which validates the interval when the
 * range itself is created.
 *
 * @param scheduleId the slot to replace, must not be null when validated
 * @param range the local time range, must not be null when validated
 * @param dayOfWeek the recurring weekday, must not be null when validated
 */
public record UpdateScheduleCommand(
        @NotNull ScheduleId scheduleId,
        @NotNull LocalTimeRange range,
        @NotNull DayOfWeek dayOfWeek) {
    /**
     * Creates a command from the slot identifier and an already validated time range. Required
     * fields are checked by Bean Validation, not here.
     *
     * @param scheduleId the slot to replace
     * @param range the validated local time range
     * @param dayOfWeek the recurring weekday
     * @return the command
     */
    public static UpdateScheduleCommand of(
            ScheduleId scheduleId, LocalTimeRange range, DayOfWeek dayOfWeek) {
        return new UpdateScheduleCommand(scheduleId, range, dayOfWeek);
    }
}
