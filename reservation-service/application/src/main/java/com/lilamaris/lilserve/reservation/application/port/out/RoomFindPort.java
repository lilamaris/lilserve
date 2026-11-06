package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.util.Optional;

/**
 * Finds rooms by identifier and deletion state within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when no room matches and never return {@code
 * null}. The caller decides how to handle a missing room.
 *
 * @see RoomSavePort
 */
public interface RoomFindPort {

    /**
     * Finds a room by identifier, deleted rooms included.
     *
     * @param roomId the room identifier, not null
     * @return the room with its deletion state, or empty when no room has this identifier
     * @throws NullPointerException if {@code roomId} is null
     */
    Optional<Room> findById(RoomId roomId);

    /**
     * Finds a room by identifier when it is not deleted. Use this for operations that must reject a
     * deleted room, such as update, delete, and find.
     *
     * @param roomId the room identifier, not null
     * @return the room, or empty when it does not exist or is deleted
     * @throws NullPointerException if {@code roomId} is null
     */
    Optional<Room> findNotDeletedById(RoomId roomId);

    /**
     * Finds a room by identifier when it is deleted. Use this for restore, which needs the deleted
     * room itself.
     *
     * @param roomId the room identifier, not null
     * @return the deleted room, or empty when it does not exist or is not deleted
     * @throws NullPointerException if {@code roomId} is null
     */
    Optional<Room> findDeletedById(RoomId roomId);
}
