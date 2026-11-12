package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Schedule;
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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence operations for recurring slots. */
public interface ScheduleRepository extends JpaRepository<Schedule, ScheduleId> {
    /**
     * Records the retirement time on slots that are not retired yet.
     *
     * <p>Flushes the persistence context before the update runs, and leaves slots that are already
     * retired untouched with their original retirement times.
     *
     * @param scheduleId the identifier value of the slot to retire
     * @param deletedAt the retirement time
     * @return the number of slots updated, zero when the slot is absent or already retired
     */
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE schedule s
                    SET deleted_at = :deletedAt
                    WHERE s.id = :scheduleId
                        AND s.deleted_at IS NULL
                    """,
            nativeQuery = true)
    int markAsDeletedByScheduleId(
            @Param("scheduleId") UUID scheduleId, @Param("deletedAt") Instant deletedAt);

    /**
     * Loads existing slots with shared locks in ascending identifier order.
     *
     * <p>Locks are retained until the caller's transaction ends. Missing identifiers are omitted;
     * the adapter checks that every requested slot was found. Retired slots remain eligible for
     * locking so callers can validate their state after acquisition.
     *
     * @param scheduleIds the slot identifiers to lock
     * @return existing slots ordered by identifier, including retired slots
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            SELECT s
            FROM Schedule s
            WHERE s.id IN (:scheduleIds)
            ORDER BY s.id
            """)
    List<Schedule> findByIdsWithSharedLock(
            @Param("scheduleIds") Collection<ScheduleId> scheduleIds);

    /**
     * Loads the slot, including a retired slot, under an exclusive row lock in one statement. The
     * caller must have an active transaction; the lock is retained until it ends.
     *
     * @param scheduleId the slot identifier to lock
     * @return the locked slot including its retirement state, or empty if no slot matches
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT s
            FROM Schedule s
            WHERE s.id = :scheduleId
            """)
    Optional<Schedule> findByIdWithExclusiveLock(@Param("scheduleId") ScheduleId scheduleId);

    /**
     * Loads non-deleted slots for one room without imposing an order.
     *
     * @param roomId the owning room identifier
     * @return the room slots, possibly empty
     */
    @Query(
            """
            SELECT s
            FROM Schedule s
            WHERE s.roomId = :roomId
                AND s.deletedAt IS NULL""")
    List<Schedule> findActiveByRoomId(@Param("roomId") RoomId roomId);
}
