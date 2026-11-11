package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;

/** Persists new blackouts and changes to existing blackouts. */
public interface BlackoutScheduleSavePort {
    /**
     * Saves a blackout within the caller's transaction without committing independently.
     *
     * <p>The caller owns lock acquisition and business validation. Constraint failures may surface
     * when this operation, a later flush, or transaction completion writes the row.
     *
     * @param blackoutSchedule the blackout to persist, not null
     */
    void save(BlackoutSchedule blackoutSchedule);
}
