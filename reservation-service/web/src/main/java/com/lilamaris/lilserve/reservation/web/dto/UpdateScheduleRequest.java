package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The JSON body that replaces a recurring room slot. Slot definitions are append-only, so the
 * replacement is offered under a new identifier and the original slot is retired; the times of the
 * original are never edited in place. The parent room is inherited from the slot being replaced, so
 * it is not part of this body.
 *
 * @param dayOfWeek the ISO weekday the replacement repeats on, not null
 * @param startAt the inclusive local start time, not null
 * @param endAt the exclusive local end time, not null
 */
@Schema(
        description = "The body that replaces a recurring room slot.",
        example = "{\"dayOfWeek\":\"TUESDAY\",\"startAt\":\"10:00:00\",\"endAt\":\"12:00:00\"}")
public record UpdateScheduleRequest(
        @Schema(
                        description = "ISO weekday the replacement repeats on",
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
     * Converts this body and the path identifier to the replacement command. Required fields are
     * checked by Bean Validation at the web boundary; the interval ordering is checked by the time
     * range built here.
     *
     * @param scheduleId the slot to replace
     * @return the replacement command
     * @throws NullPointerException if {@code scheduleId} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endAt} is not after {@code startAt}
     */
    public UpdateScheduleCommand toCommand(UUID scheduleId) {
        return UpdateScheduleCommand.of(
                ScheduleId.of(scheduleId), LocalTimeRange.of(startAt, endAt), dayOfWeek);
    }
}
