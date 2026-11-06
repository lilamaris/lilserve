package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A request to create one recurring room slot.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * <p>The factory validates the local time range immediately.
 *
 * @param roomId the room offering the slot, must not be null when validated
 * @param range the local time range, must not be null when validated
 * @param dayOfWeek the recurring weekday, must not be null when validated
 */
public record CreateScheduleCommand(
        @NotNull RoomId roomId, @NotNull LocalTimeRange range, @NotNull DayOfWeek dayOfWeek) {
    /**
     * Creates a command with a validated local time range.
     *
     * @param roomId the room offering the slot
     * @param startAt the inclusive start time
     * @param endAt the exclusive end time, strictly after the start time
     * @param dayOfWeek the recurring weekday
     * @return the command
     * @throws NullPointerException if {@code startAt} or {@code endAt} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endAt} is not after {@code startAt}
     */
    public static CreateScheduleCommand of(
            RoomId roomId, LocalTime startAt, LocalTime endAt, DayOfWeek dayOfWeek) {
        var range = LocalTimeRange.of(startAt, endAt);
        return new CreateScheduleCommand(roomId, range, dayOfWeek);
    }
}
