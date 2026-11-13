package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotNull;

/**
 * Requests a replacement room usage interval for an existing blackout.
 *
 * <p>Construction and the factory retain the supplied values without running Bean Validation.
 * Required fields and text constraints are checked by explicit validation or a validated use case.
 * A non-null {@link InstantRange} validates its own bounds when constructed.
 *
 * @param blackoutScheduleId the blackout to change, required when validated
 * @param period the replacement absolute usage interval, inclusive at the start and exclusive at
 *     the end, required when validated
 */
public record UpdateBlackoutSchedulePeriodCommand(
        @NotNull BlackoutScheduleId blackoutScheduleId, @NotNull InstantRange period) {
    /**
     * Creates a command without running Bean Validation.
     *
     * @param blackoutScheduleId the blackout to change
     * @param period the replacement absolute usage interval, inclusive at the start and exclusive
     *     at the end
     * @return the command
     */
    public static UpdateBlackoutSchedulePeriodCommand of(
            BlackoutScheduleId blackoutScheduleId, InstantRange period) {
        return new UpdateBlackoutSchedulePeriodCommand(blackoutScheduleId, period);
    }
}
