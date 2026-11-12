package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One requested slot inside a batch create body. A batch carries a flat list of these items, so the
 * caller does not group weekdays into a map; {@link CreateSchedulesRequest#toCommand()} builds the
 * grouped map the use case asks for.
 *
 * @param dayOfWeek the ISO weekday the slot repeats on, not null
 * @param startAt the inclusive local start time, not null
 * @param endAt the exclusive local end time, not null
 */
@Schema(
        description = "One slot inside a batch create body.",
        example = "{\"dayOfWeek\":\"MONDAY\",\"startAt\":\"09:00:00\",\"endAt\":\"11:00:00\"}")
public record ScheduleSlotRequest(
        @Schema(
                        description = "ISO weekday the slot repeats on",
                        type = "string",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                DayOfWeek dayOfWeek,
        @Schema(
                        description = "inclusive local start time",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                LocalTime startAt,
        @Schema(
                        description = "exclusive local end time, after startAt",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                LocalTime endAt) {

    /**
     * Builds the validated time range of this slot.
     *
     * @return the local time range
     * @throws NullPointerException if {@code startAt} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endAt} is not after {@code startAt}
     */
    public LocalTimeRange toRange() {
        return LocalTimeRange.of(startAt, endAt);
    }
}
