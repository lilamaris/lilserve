package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Optional;
import java.util.Set;

/**
 * Locks recurring slots by identifier within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when any requested slot is missing and never
 * return {@code null}. The caller decides how to handle a partial lock.
 *
 * @see ScheduleFindPort
 * @see ScheduleSavePort
 */
public interface ScheduleLockFindPort {

    /**
     * Locks the requested slots and keeps their shared locks until the caller's transaction ends.
     * Use this for operations that validate slots before allocating, such as reservation.
     *
     * <p>Requires an active transaction. Rows are locked in ascending identifier order, so
     * competing transactions lock rows in the same order, and a retired slot is locked like an
     * active slot. Every locked row is reported as its identifier, and a missing row is reported as
     * an empty result.
     *
     * @param scheduleIds the slot identifiers, non-empty and without null elements
     * @return the locked slot identifiers, including retired slots, or empty when any requested
     *     slot is missing
     */
    Optional<Set<ScheduleId>> findAllLockedById(Set<ScheduleId> scheduleIds);
}
