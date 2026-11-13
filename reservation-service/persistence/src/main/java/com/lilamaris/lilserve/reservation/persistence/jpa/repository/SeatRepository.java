package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence operations for seats. */
public interface SeatRepository extends JpaRepository<Seat, SeatId> {
    /**
     * Loads the seat with a shared lock on its row.
     *
     * <p>The lock is requested by the same statement that loads the row and is retained until the
     * caller's transaction ends. A deleted seat is loaded like an active one, so callers can
     * validate its state after acquisition.
     *
     * @param seatId the seat identifier to lock
     * @return the locked seat, including a deleted seat, or empty when no seat has this identifier
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            SELECT s
            FROM Seat s
            WHERE s.id = :seatId""")
    Optional<Seat> findByIdWithSharedLock(@Param("seatId") SeatId seatId);

    /**
     * Loads the seat, including a deleted seat, under an exclusive row lock in one statement. The
     * caller must have an active transaction; the lock is retained until it ends.
     *
     * @param seatId the seat identifier to lock
     * @return the locked seat including its deletion state, or empty if no seat matches
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT s
            FROM Seat s
            WHERE s.id = :seatId""")
    Optional<Seat> findByIdWithExclusiveLock(@Param("seatId") SeatId seatId);

    /**
     * Finds active seats in one room with database pagination.
     *
     * @param roomId the owning room identifier
     * @param pageable the database page bounds
     * @return matching seats ordered by code and identifier with the room's active seat total
     */
    @Query(
            """
            SELECT s FROM Seat s
            WHERE s.roomId = :roomId AND s.deletedAt IS NULL
            ORDER BY s.code, s.id
            """)
    Page<Seat> findActiveByRoomId(@Param("roomId") RoomId roomId, Pageable pageable);

    /**
     * Finds non-deleted seats with no active allocation overlapping any selected slot.
     *
     * <p>Selected local slot times are combined with the service date and supplied timezone.
     * Existing allocations are compared by their stored absolute instants, even when their original
     * slots are retired. Half-open intervals allow adjacent reservations and preserve gaps between
     * selected slots. The caller validates slot ownership, weekday, and intake.
     *
     * @param roomId the owning room UUID
     * @param targetDate the requested service date
     * @param scheduleIds non-empty selected slot UUID values
     * @param timeZone the timezone name used to interpret selected slot times
     * @return available seat entities ordered by code and identifier
     */
    @Query(
            value =
                    """
            SELECT s.*
            FROM seat s
            WHERE s.room_id = :roomId
                AND s.deleted_at IS NULL
                AND NOT EXISTS (
                    SELECT 1
                    FROM reserved_schedule rs
                    WHERE rs.seat_id = s.id
                        AND rs.deleted_at IS NULL
                        AND EXISTS (
                            SELECT 1
                            FROM schedule selected
                            WHERE selected.id IN (:scheduleIds)
                                AND rs.starts_at < (
                                    (CAST(:targetDate AS date) + selected.end_at)
                                    AT TIME ZONE CAST(:timeZone AS text)
                                )
                                AND (
                                    (CAST(:targetDate AS date) + selected.start_at)
                                    AT TIME ZONE CAST(:timeZone AS text)
                                ) < rs.ends_at
                        )
                )
            ORDER BY s.code, s.id""",
            nativeQuery = true)
    List<Seat> findAvailable(
            @Param("roomId") UUID roomId,
            @Param("targetDate") LocalDate targetDate,
            @Param("scheduleIds") Collection<UUID> scheduleIds,
            @Param("timeZone") String timeZone);
}
