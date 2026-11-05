package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** A request to create one recurring slot, validated at the service boundary. */
public record CreateScheduleCommand(
        @NotNull RoomId roomId, @NotNull LocalTimeRange range, @NotNull DayOfWeek dayOfWeek) {
    /**
     * Creates a request with a validated local time range.
     *
     * @param roomId the room offering the slot
     * @param startAt the inclusive start time
     * @param endAt the exclusive end time, strictly after the start time
     * @param dayOfWeek the recurring weekday
     * @return the request
     */
    public static CreateScheduleCommand of(
            RoomId roomId, LocalTime startAt, LocalTime endAt, DayOfWeek dayOfWeek) {
        var range = LocalTimeRange.of(startAt, endAt);
        return new CreateScheduleCommand(roomId, range, dayOfWeek);
    }
}
