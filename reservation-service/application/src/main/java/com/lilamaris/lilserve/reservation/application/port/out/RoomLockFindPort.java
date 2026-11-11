package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.util.Optional;

/**
 * Finds a room by identifier under a row lock within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when no room matches and never return {@code
 * null}. The caller decides how to handle a missing room.
 *
 * @see RoomFindPort
 * @see RoomSavePort
 */
public interface RoomLockFindPort {

    /**
     * Finds a room by identifier and keeps a shared lock on its row until the caller's transaction
     * ends. Use this for operations that validate a room before writing, such as reservation or
     * schedule creation.
     *
     * <p>Requires an active transaction. The lock and the read are requested by one statement, so
     * the returned deletion state is the state this transaction protects. A deleted room is
     * returned with that state, and a missing row is reported as an empty result.
     *
     * @param roomId the room identifier, not null
     * @return the locked room with its deletion state, or empty when no room has this identifier
     * @throws NullPointerException if {@code roomId} is null
     */
    Optional<Room> findByIdWithSharedLock(RoomId roomId);

    /**
     * Finds a room by identifier under an exclusive row lock until the caller's transaction ends.
     * Competing room changes must wait and validate the state returned after acquiring the lock.
     *
     * <p>Requires an active transaction. The lock and read use one statement. Deleted rooms are
     * included so callers can decide whether modification, deletion, or restoration is allowed.
     *
     * @param roomId the room identifier, not null
     * @return the locked room including its deletion state, or empty if no room matches
     * @throws NullPointerException if {@code roomId} is null
     */
    Optional<Room> findByIdWithExclusiveLock(RoomId roomId);
}
