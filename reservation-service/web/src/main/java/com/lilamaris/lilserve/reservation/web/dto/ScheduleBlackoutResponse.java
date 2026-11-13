package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.schedule.BlackoutScheduleSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * A reservation intake blackout as a slot list reports it. Only the intake interval the slot
 * carries is published, so the blackout's own creation, update, and deletion timestamps are absent:
 * a slot list answers the blackouts that are active at the requested instant, and those rows are
 * never deleted at that instant.
 *
 * @param blackoutScheduleId the blackout identifier
 * @param startsAt the inclusive intake start instant
 * @param endsAt the exclusive intake end instant
 * @param reason the blackout reason
 */
@Schema(
        description =
                "A blackout as a slot list reports it: only the intake interval is published, so"
                        + " the blackout's creation, update, and deletion timestamps are absent.",
        example = OpenApiExamples.SCHEDULE_BLACKOUT)
public record ScheduleBlackoutResponse(
        @Schema(
                        description = "blackout identifier",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID blackoutScheduleId,
        @Schema(
                        description = "inclusive intake start instant",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant startsAt,
        @Schema(
                        description = "exclusive intake end instant",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant endsAt,
        @Schema(
                        description = "blackout reason",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                String reason) {

    /**
     * Maps one blackout of a listed slot.
     *
     * @param summary the active blackout
     * @return the nested blackout body
     * @throws NullPointerException if {@code summary} is null
     */
    public static ScheduleBlackoutResponse from(BlackoutScheduleSummary summary) {
        return new ScheduleBlackoutResponse(
                summary.blackoutScheduleId().getValue(),
                summary.validPeriod().startsAt(),
                summary.validPeriod().endsAt(),
                summary.reason());
    }
}
