package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The JSON body that creates recurring room slots in one batch. The requested slots are carried as
 * a flat list, and the weekday map the use case asks for is built here, so a batch body does not
 * expose the map shape. The list must not be empty; the whole batch is created atomically by the
 * use case.
 *
 * @param roomId the room offering the slots, not null
 * @param schedules the requested slots, non-empty, every item validated
 */
@Schema(
        description = "The body that creates recurring room slots in one batch.",
        example =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","schedules":[\
                {"dayOfWeek":"MONDAY","startAt":"09:00:00","endAt":"11:00:00"}]}
                """)
public record CreateSchedulesRequest(
        @Schema(
                        description = "room offering the slots",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                UUID roomId,
        @Schema(
                        description = "requested slots, at least one",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotEmpty
                List<@Valid ScheduleSlotRequest> schedules) {

    /**
     * Groups this body's slots by weekday into the command the use case asks for. Required fields
     * are checked by Bean Validation at the web boundary; each item's interval ordering is checked
     * by the time range built here.
     *
     * @return the batch create command
     * @throws NullPointerException if {@code roomId} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if an
     *     item's end time is not after its start time
     */
    public CreateSchedulesCommand toCommand() {
        return CreateSchedulesCommand.of(RoomId.of(roomId), groupByWeekday());
    }

    private Map<DayOfWeek, List<LocalTimeRange>> groupByWeekday() {
        var grouped = new EnumMap<DayOfWeek, List<LocalTimeRange>>(DayOfWeek.class);
        for (var slot : schedules) {
            rangesOf(grouped, slot.dayOfWeek()).add(slot.toRange());
        }
        return Map.copyOf(grouped);
    }

    private List<LocalTimeRange> rangesOf(
            Map<DayOfWeek, List<LocalTimeRange>> grouped, DayOfWeek dayOfWeek) {
        var ranges = grouped.get(dayOfWeek);
        if (ranges == null) {
            ranges = new ArrayList<>();
            grouped.put(dayOfWeek, ranges);
        }
        return ranges;
    }
}
