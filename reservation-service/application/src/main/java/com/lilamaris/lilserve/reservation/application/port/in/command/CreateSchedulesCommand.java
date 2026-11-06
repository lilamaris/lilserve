package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request to create recurring room slots grouped by weekday.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * <p>The weekday map and its range lists are copied at construction; null collections, keys,
 * values, and range elements are rejected immediately.
 *
 * @param roomId the room offering the slots, must not be null when validated
 * @param schedules the requested ranges grouped by weekday, non-empty when validated
 */
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
     * Creates a command with immutable weekday and time range snapshots.
     *
     * @param roomId the room offering the slots
     * @param schedules the requested ranges; must be non-empty when validated
     * @return the command
     * @throws NullPointerException if {@code schedules}, any key, value, or range element is null
     */
    public static CreateSchedulesCommand of(
            RoomId roomId, Map<DayOfWeek, List<LocalTimeRange>> schedules) {
        return new CreateSchedulesCommand(roomId, Map.copyOf(schedules));
    }
}
