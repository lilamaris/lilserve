package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Slot requests grouped by weekday; service entry requires a non-empty map and a room. */
public record CreateSchedulesCommand(
        @NotNull RoomId roomId,
        @NotEmpty Map<@NotNull DayOfWeek, @NotNull List<@NotNull LocalTimeRange>> schedules) {
    /**
     * Copies both the weekday map and every range list into immutable snapshots.
     *
     * @param roomId the room offering the slots
     * @param schedules the weekday map; null keys, values and range elements are rejected
     */
    public CreateSchedulesCommand {
        var copy = new HashMap<DayOfWeek, List<LocalTimeRange>>();
        schedules.forEach((day, ranges) -> copy.put(day, List.copyOf(ranges)));
        schedules = Map.copyOf(copy);
    }

    /**
     * Creates a request with immutable weekday and time range snapshots.
     *
     * @param roomId the room offering the slots
     * @param schedules the requested ranges; must be non-empty at the service boundary
     * @return the request
     */
    public static CreateSchedulesCommand of(
            RoomId roomId, Map<DayOfWeek, List<LocalTimeRange>> schedules) {
        return new CreateSchedulesCommand(roomId, Map.copyOf(schedules));
    }
}
