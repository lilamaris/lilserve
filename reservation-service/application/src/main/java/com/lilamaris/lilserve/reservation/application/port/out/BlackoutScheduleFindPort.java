package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads reservation intake blackouts without acquiring row locks.
 *
 * <p>Queries return an existence decision or a scalar parent identifier without loading a blackout
 * entity. The caller coordinates concurrent intake changes through the parent schedule lock.
 */
public interface BlackoutScheduleFindPort {
    /**
     * Checks whether any selected slot has a non-deleted blackout at the intake instant.
     *
     * <p>The interval is half-open: the start is blocked and the end is allowed. This compares the
     * reservation intake instant, independent of the requested service date and slot usage times.
     * For a decision protected against concurrent blackout creation, period changes, or deletion,
     * retain shared locks on all selected schedules before querying, through reservation
     * completion.
     *
     * @param scheduleIds the selected slot identifiers, non-empty and without null elements
     * @param now the reservation intake instant, not null
     * @return true if at least one selected slot is blocked at the given instant
     */
    boolean existsActiveByScheduleIdsAt(Set<ScheduleId> scheduleIds, Instant now);

    /**
     * Reads only the immutable parent schedule identifier, including for a deleted blackout.
     *
     * <p>Does not load or cache a blackout entity. Use the result to lock the parent schedule
     * first, then lock the blackout and validate its current deletion state before changing its
     * intake period or deleting it. An empty result denotes a missing blackout, never a null return
     * value.
     *
     * @param blackoutScheduleId the blackout identifier, not null
     * @return the parent identifier, or empty when the blackout does not exist
     */
    Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId);

    /**
     * Loads non-deleted room blackouts whose half-open interval contains the instant. Multiple
     * overlapping blackouts for the same slot are retained.
     *
     * @param roomId the owning room identifier
     * @param now the reservation intake instant
     * @return the active blackouts in unspecified order, or an empty list
     */
    List<BlackoutSchedule> findNotDeletedByRoomIdAt(RoomId roomId, Instant now);
}
