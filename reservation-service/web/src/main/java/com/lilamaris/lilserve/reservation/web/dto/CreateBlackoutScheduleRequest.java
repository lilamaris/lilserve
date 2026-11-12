package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON body that suspends new reservation intake for one recurring room slot. The interval is
 * absolute and applies to the intake instant, independent of the requested service date, so it
 * carries instants rather than local times. The room and the slot are carried by the body, as a
 * seat carries its room; persistence enforces that the slot belongs to that room.
 *
 * <p>Overlapping blackouts are allowed, and existing reservations are preserved.
 *
 * @param roomId the room offering the slot, not null
 * @param scheduleId the slot whose intake is blocked, not null
 * @param startsAt the inclusive start instant, not null
 * @param endsAt the exclusive end instant, not null
 * @param reason the explanation, non-blank and within {@link BlackoutSchedule#MAX_REASON_LENGTH}
 */
@Schema(
        description = "The body that suspends new reservation intake for one recurring room slot.",
        example =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","scheduleId":"00000000-0000-0000-\
                0000-000000000003","startsAt":"2026-01-02T00:00:00Z","endsAt":"2026-01-03T00:00:00Z",\
                "reason":"facility maintenance"}
                """)
public record CreateBlackoutScheduleRequest(
        @Schema(
                        description = "room offering the slot",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                UUID roomId,
        @Schema(
                        description = "slot whose intake is blocked",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                UUID scheduleId,
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
                Instant endsAt,
        @Schema(
                        description = "blackout reason",
                        maxLength = BlackoutSchedule.MAX_REASON_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = BlackoutSchedule.MAX_REASON_LENGTH)
                String reason) {

    /**
     * Converts this body to the create command. Required fields and the reason length are checked
     * by Bean Validation at the web boundary; the interval ordering is checked by the absolute
     * range built here.
     *
     * @return the create command
     * @throws NullPointerException if {@code roomId} or {@code scheduleId} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endsAt} is not after {@code startsAt}
     */
    public CreateBlackoutScheduleCommand toCommand() {
        return CreateBlackoutScheduleCommand.of(
                RoomId.of(roomId),
                ScheduleId.of(scheduleId),
                InstantRange.of(startsAt, endsAt),
                reason);
    }
}
