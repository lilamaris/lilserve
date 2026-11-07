package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;

/**
 * Retires recurring slots by recording their retirement time within the caller's transaction.
 *
 * <p>Slot definitions are append-only and are never removed from storage, so a retired slot stays
 * readable and keeps the allocations that reference it.
 *
 * @see ScheduleFindPort
 * @see ScheduleSavePort
 */
public interface RetireSchedulePort {
    /**
     * Retires a slot that is not retired yet. A slot that is already retired is left unchanged with
     * its original retirement time. The caller owns the transaction, and persistence failures
     * propagate to the caller so the whole operation can be rolled back.
     *
     * @param scheduleId the slot to retire, not null
     * @param retiredAt the retirement instant, not null
     */
    void retire(ScheduleId scheduleId, Instant retiredAt);
}
