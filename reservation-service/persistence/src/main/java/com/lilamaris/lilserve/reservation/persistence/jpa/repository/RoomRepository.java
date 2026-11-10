package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence operations for rooms. */
public interface RoomRepository extends JpaRepository<Room, RoomId> {
    /**
     * Loads the room with a shared lock on its row.
     *
     * <p>The lock is requested by the same statement that loads the row and is retained until the
     * caller's transaction ends. A deleted room is loaded like an active one, so callers can
     * validate its state after acquisition.
     *
     * @param roomId the room identifier to lock
     * @return the locked room, including a deleted room, or empty when no room has this identifier
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            SELECT r
            FROM Room r
            WHERE r.id = :roomId""")
    Optional<Room> findByIdWithSharedLock(@Param("roomId") RoomId roomId);
}
