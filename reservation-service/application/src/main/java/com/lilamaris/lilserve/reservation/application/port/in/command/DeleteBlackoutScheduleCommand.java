package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotNull;

/**
 * Requests soft deletion of a reservation intake blackout.
 *
 * <p>Construction and the factory retain the supplied values without running Bean Validation.
 * Constraints are checked by explicit validation or a validated use case.
 *
 * @param blackoutScheduleId the blackout to delete, required when validated
 */
public record DeleteBlackoutScheduleCommand(@NotNull BlackoutScheduleId blackoutScheduleId) {
    /**
     * Creates a command without running Bean Validation.
     *
     * @param blackoutScheduleId the blackout to delete
     * @return the command
     */
    public static DeleteBlackoutScheduleCommand of(BlackoutScheduleId blackoutScheduleId) {
        return new DeleteBlackoutScheduleCommand(blackoutScheduleId);
    }
}
