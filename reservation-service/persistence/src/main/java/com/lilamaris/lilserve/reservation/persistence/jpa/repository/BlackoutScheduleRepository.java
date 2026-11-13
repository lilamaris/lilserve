package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence, locked reads, and room usage queries for blackouts. */
public interface BlackoutScheduleRepository
        extends JpaRepository<BlackoutSchedule, BlackoutScheduleId> {
    /**
     * Reads a blackout, including a deleted row, with an exclusive lock in one statement.
     *
     * <p>Requires an active transaction; the lock is held until its end. Usage-changing callers
     * must lock the parent room and then the registration schedule before acquiring this lock.
     *
     * @param blackoutScheduleId the blackout identifier, not null
     * @return the locked blackout, or empty if absent
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT bs
            FROM BlackoutSchedule bs
            WHERE bs.id = :blackoutScheduleId""")
    Optional<BlackoutSchedule> findByIdWithExclusiveLock(
            @Param("blackoutScheduleId") BlackoutScheduleId blackoutScheduleId);

    /**
     * Projects the parent identifier without loading the blackout entity or locking either row.
     * Deleted blackouts are included so a later protected read can decide whether a change is
     * allowed.
     *
     * @param blackoutScheduleId the blackout identifier, not null
     * @return the parent identifier, or empty if the blackout is absent
     */
    @Query(
            """
            SELECT bs.scheduleId
            FROM BlackoutSchedule bs
            WHERE bs.id = :blackoutScheduleId""")
    Optional<ScheduleId> findScheduleIdById(
            @Param("blackoutScheduleId") BlackoutScheduleId blackoutScheduleId);

    /**
     * Checks each requested service interval against all non-deleted blackouts of its room.
     *
     * <p>Uses half-open timestamp ranges and the room blackout GiST index. The registration
     * schedule of a blackout does not limit its room-wide effect. The caller owns locking.
     *
     * @param roomId the owning room UUID
     * @param scheduleIds non-empty selected slot UUIDs
     * @param serviceDate the requested service date
     * @param timeZone the reservation timezone name
     * @return whether any requested interval overlaps a room restriction
     */
    @Query(
            value =
                    """
            SELECT EXISTS (
                SELECT 1
                FROM blackout_schedule bs
                JOIN schedule s ON s.room_id = bs.room_id
                WHERE bs.room_id = :roomId
                    AND bs.deleted_at IS NULL
                    AND s.id IN (:scheduleIds)
                    AND s.deleted_at IS NULL
                    AND s.day_of_week = EXTRACT(ISODOW FROM CAST(:serviceDate AS date))
                    AND tstzrange(bs.starts_at, bs.ends_at, '[)') && tstzrange(
                        (CAST(:serviceDate AS date) + s.start_at) AT TIME ZONE CAST(:timeZone AS text),
                        (CAST(:serviceDate AS date) + s.end_at) AT TIME ZONE CAST(:timeZone AS text),
                        '[)')
            )""",
            nativeQuery = true)
    boolean existsOverlappingUsage(
            @Param("roomId") UUID roomId,
            @Param("scheduleIds") Collection<UUID> scheduleIds,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("timeZone") String timeZone);

    /**
     * Projects the immutable owning room, including for a deleted blackout, without a row lock.
     *
     * @param blackoutScheduleId the blackout identifier
     * @return the owning room, or empty if the blackout is absent
     */
    @Query("SELECT bs.roomId FROM BlackoutSchedule bs WHERE bs.id = :blackoutScheduleId")
    Optional<RoomId> findRoomIdById(
            @Param("blackoutScheduleId") BlackoutScheduleId blackoutScheduleId);

    /**
     * Loads all non-deleted room blackouts active at the supplied instant, including overlaps.
     *
     * @param roomId the owning room identifier
     * @param at the inclusive-start, exclusive-end usage instant
     * @return the matching blackouts in unspecified order
     */
    @Query(
            """
            SELECT bs
            FROM BlackoutSchedule bs
            WHERE bs.roomId = :roomId
                AND bs.deletedAt IS NULL
                AND bs.startsAt <= :at
                AND :at < bs.endsAt""")
    List<BlackoutSchedule> findActiveByRoomIdAt(
            @Param("roomId") RoomId roomId, @Param("at") Instant at);
}
