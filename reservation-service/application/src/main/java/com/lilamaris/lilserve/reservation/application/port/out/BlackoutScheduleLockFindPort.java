package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import java.util.Optional;

/** Locks blackout rows within the caller's transaction. */
public interface BlackoutScheduleLockFindPort {
    /**
     * Reads a blackout, including a deleted one, under an exclusive row lock.
     *
     * <p>Requires an active transaction and retains the lock until it ends. The caller validates
     * the returned deletion state. For period changes and deletion, acquire the parent schedule's
     * exclusive lock first to coordinate with reservation intake checks.
     *
     * @param blackoutScheduleId the blackout identifier, not null
     * @return the locked blackout, or empty if missing; never null
     */
    Optional<BlackoutSchedule> findByIdWithExclusiveLock(BlackoutScheduleId blackoutScheduleId);
}
