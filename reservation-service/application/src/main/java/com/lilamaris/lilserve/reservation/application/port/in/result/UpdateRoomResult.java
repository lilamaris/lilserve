package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;

/**
 * A room after a change. Every change moves {@code updatedAt} forward, and deletion is recorded in
 * {@code deletedAt}.
 *
 * @param roomId the room identifier
 * @param code the room code
 * @param name the room name
 * @param description the room description
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
public record UpdateRoomResult(
        RoomId roomId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    /**
     * Maps a room to the result.
     *
     * @param room the changed room
     * @return the result
     * @throws NullPointerException if {@code room} is null
     */
    public static UpdateRoomResult from(Room room) {
        return new UpdateRoomResult(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                room.getDeletedAt());
    }
}
