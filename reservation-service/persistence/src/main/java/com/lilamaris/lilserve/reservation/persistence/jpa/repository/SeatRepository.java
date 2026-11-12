package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
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
}
