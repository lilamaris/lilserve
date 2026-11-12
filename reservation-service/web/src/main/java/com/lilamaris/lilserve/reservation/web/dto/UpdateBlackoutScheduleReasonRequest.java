package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The JSON body that replaces the explanation of an existing blackout. Changing the reason does not
 * change the intake interval, so it is mapped on its own endpoint.
 *
 * @param reason the explanation, non-blank and within {@link BlackoutSchedule#MAX_REASON_LENGTH}
 */
@Schema(
        description = "The body that replaces a blackout's reason.",
        example = "{\"reason\":\"moved to next week\"}")
public record UpdateBlackoutScheduleReasonRequest(
        @Schema(
                        description = "new blackout reason",
                        maxLength = BlackoutSchedule.MAX_REASON_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = BlackoutSchedule.MAX_REASON_LENGTH)
                String reason) {

    /**
     * Converts this body and the path identifier to the reason command. Value constraints are
     * checked by Bean Validation at the web boundary.
     *
     * @param blackoutScheduleId the blackout to change
     * @return the reason command
     * @throws NullPointerException if {@code blackoutScheduleId} is null
     */
    public UpdateBlackoutScheduleReasonCommand toCommand(UUID blackoutScheduleId) {
        return UpdateBlackoutScheduleReasonCommand.of(
                BlackoutScheduleId.of(blackoutScheduleId), reason);
    }
}
