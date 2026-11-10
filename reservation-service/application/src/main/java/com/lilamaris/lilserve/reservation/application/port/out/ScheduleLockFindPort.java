package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Optional;
import java.util.Set;

/**
 * Locks recurring slots by identifier within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when a requested slot is missing and never
 * return {@code null}. The caller decides how to handle a missing slot or a partial lock.
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
    Optional<Set<ScheduleId>> findAllByIdsWithSharedLock(Set<ScheduleId> scheduleIds);

    /**
     * Finds a slot by identifier under an exclusive row lock until the caller's transaction ends.
     * Competing replacements must wait and validate the state returned after acquiring the lock.
     *
     * <p>Requires an active transaction. The lock and read use one statement. Retired slots are
     * included so callers can reject requests after a preceding replacement has committed.
     *
     * @param scheduleId the slot identifier, not null
     * @return the locked slot including its retirement state, or empty if no slot matches
     * @throws NullPointerException if {@code scheduleId} is null
     */
    Optional<Schedule> findByIdWithExclusiveLock(ScheduleId scheduleId);
}
