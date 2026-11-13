package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads physical room usage restrictions without acquiring row locks.
 *
 * <p>Queries return an existence decision or a scalar parent identifier without loading a blackout
 * entity. The caller coordinates concurrent usage changes through the parent room lock.
 */
public interface BlackoutScheduleFindPort {
    /**
     * Checks whether any selected service interval overlaps a non-deleted room restriction.
     *
     * <p>Combines each selected slot with the service date and timezone. Half-open intervals allow
     * adjacent usage and preserve gaps between selected slots. All blackouts of the room apply,
     * regardless of their registration schedule. Reservation writers retain the shared room lock
     * until completion; blackout period mutations acquire the same room exclusively.
     *
     * @param roomId the requested room
     * @param scheduleIds non-empty selected slot identifiers
     * @param serviceDate the requested usage date
     * @param timeZone the timezone used by reservation creation
     * @return true if at least one selected usage interval is blocked
     */
    boolean existsOverlappingUsage(
            RoomId roomId, Set<ScheduleId> scheduleIds, LocalDate serviceDate, ZoneId timeZone);

    /**
     * Projects the immutable owning room without loading or locking the blackout entity.
     *
     * @param blackoutScheduleId the blackout identifier
     * @return the owning room, including for deleted blackouts, or empty if absent
     */
    Optional<RoomId> findRoomIdById(BlackoutScheduleId blackoutScheduleId);

    /**
     * Reads only the immutable parent schedule identifier, including for a deleted blackout.
     *
     * <p>Does not load or cache a blackout entity. After locking the owning room, use this result
     * to lock the registration schedule, then lock the blackout and validate its current deletion
     * state before changing its usage period or deleting it. An empty result denotes a missing
     * blackout, never a null return value.
     *
     * @param blackoutScheduleId the blackout identifier, not null
     * @return the parent identifier, or empty when the blackout does not exist
     */
    Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId);

    /**
     * Loads non-deleted room blackouts whose half-open interval contains the instant. Multiple
     * overlapping blackouts for the same room are retained.
     *
     * @param roomId the owning room identifier
     * @param now the instant at which room usage status is requested
     * @return the active blackouts in unspecified order, or an empty list
     */
    List<BlackoutSchedule> findNotDeletedByRoomIdAt(RoomId roomId, Instant now);
}
