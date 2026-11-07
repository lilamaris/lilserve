package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Optional;

/**
 * Finds recurring slots by identifier and deletion state within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when no slot matches and never return {@code
 * null}. The caller decides how to handle a missing slot.
 *
 * @see ScheduleSavePort
 * @see RetireSchedulePort
 */
public interface ScheduleFindPort {

    /**
     * Finds a slot by identifier when it is not retired. Use this for operations that must reject a
     * retired slot, such as update.
     *
     * @param scheduleId the slot identifier, not null
     * @return the slot, or empty when it does not exist or is retired
     */
    Optional<Schedule> findNotDeletedById(ScheduleId scheduleId);
}
