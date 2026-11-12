package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import java.time.Instant;

/**
 * The deleted blackout identifier and its deletion time.
 *
 * @param blackoutScheduleId the deleted blackout identifier
 * @param deletedAt the deletion instant
 */
public record DeleteBlackoutScheduleResult(
        BlackoutScheduleId blackoutScheduleId, Instant deletedAt) {
    /**
     * Creates a result snapshot from the deleted blackout.
     *
     * @param blackoutSchedule the deleted blackout
     * @return the result
     * @throws NullPointerException if {@code blackoutSchedule} is null
     */
    public static DeleteBlackoutScheduleResult from(BlackoutSchedule blackoutSchedule) {
        return new DeleteBlackoutScheduleResult(
                blackoutSchedule.getId(), blackoutSchedule.getDeletedAt());
    }
}
