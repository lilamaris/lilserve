package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requests a replacement explanation for an existing blackout.
 *
 * <p>Construction and the factory retain the supplied values without running Bean Validation.
 * Constraints are checked by explicit validation or a validated use case.
 *
 * @param blackoutScheduleId the blackout to change, required when validated
 * @param reason the non-blank explanation, at most the blackout reason length limit, required when
 *     validated
 */
public record UpdateBlackoutScheduleReasonCommand(
        @NotNull BlackoutScheduleId blackoutScheduleId,
        @NotBlank @Size(max = BlackoutSchedule.MAX_REASON_LENGTH) String reason) {
    /**
     * Creates a command without running Bean Validation.
     *
     * @param blackoutScheduleId the blackout to change
     * @param reason the non-blank explanation, at most the blackout reason length limit
     * @return the command
     */
    public static UpdateBlackoutScheduleReasonCommand of(
            BlackoutScheduleId blackoutScheduleId, String reason) {
        return new UpdateBlackoutScheduleReasonCommand(blackoutScheduleId, reason);
    }
}
