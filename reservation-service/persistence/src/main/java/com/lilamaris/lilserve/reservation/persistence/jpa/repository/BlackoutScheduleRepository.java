package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence, locked reads, and reservation intake queries for blackouts. */
public interface BlackoutScheduleRepository
        extends JpaRepository<BlackoutSchedule, BlackoutScheduleId> {
    /**
     * Reads a blackout, including a deleted row, with an exclusive lock in one statement.
     *
     * <p>Requires an active transaction; the lock is held until its end. Intake-changing callers
     * must lock the parent schedule before acquiring this lock.
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
     * Checks for any non-deleted blackout containing the reservation intake instant.
     *
     * <p>Uses PostgreSQL EXISTS and a half-open interval. This query does not acquire row locks;
     * reservation callers first retain shared locks on the selected schedules. UUID values are
     * required for the native parameters, rather than embedded schedule identifier objects.
     *
     * @param scheduleIds the slot UUIDs, non-empty and without null elements
     * @param at the reservation intake instant, not null
     * @return true if any selected slot rejects new intake at this instant
     */
    @Query(
            value =
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM blackout_schedule bs
                        WHERE bs.schedule_id IN (:scheduleIds)
                            AND bs.deleted_at IS NULL
                            AND bs.starts_at <= :at
                            AND :at < bs.ends_at
                    )""",
            nativeQuery = true)
    boolean existsActiveByScheduleIdsAt(
            @Param("scheduleIds") Collection<UUID> scheduleIds, @Param("at") Instant at);

    /**
     * Loads all non-deleted room blackouts active at the supplied instant, including overlaps.
     *
     * @param roomId the owning room identifier
     * @param at the inclusive-start, exclusive-end intake instant
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
