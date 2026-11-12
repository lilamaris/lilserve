package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON body that replaces the reservation intake interval of an existing blackout. The interval
 * is absolute, so it carries instants. Existing reservations are preserved.
 *
 * @param startsAt the inclusive start instant, not null
 * @param endsAt the exclusive end instant, not null
 */
@Schema(
        description = "The body that replaces a blackout's reservation intake interval.",
        example = "{\"startsAt\":\"2026-01-04T00:00:00Z\",\"endsAt\":\"2026-01-05T00:00:00Z\"}")
public record UpdateBlackoutSchedulePeriodRequest(
        @Schema(
                        description = "inclusive intake start instant",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                Instant startsAt,
        @Schema(
                        description = "exclusive intake end instant, after startsAt",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                Instant endsAt) {

    /**
     * Converts this body and the path identifier to the period command. Required fields are checked
     * by Bean Validation at the web boundary; the interval ordering is checked by the absolute
     * range built here.
     *
     * @param blackoutScheduleId the blackout to change
     * @return the period command
     * @throws NullPointerException if {@code blackoutScheduleId} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endsAt} is not after {@code startsAt}
     */
    public UpdateBlackoutSchedulePeriodCommand toCommand(UUID blackoutScheduleId) {
        return UpdateBlackoutSchedulePeriodCommand.of(
                BlackoutScheduleId.of(blackoutScheduleId), InstantRange.of(startsAt, endsAt));
    }
}
