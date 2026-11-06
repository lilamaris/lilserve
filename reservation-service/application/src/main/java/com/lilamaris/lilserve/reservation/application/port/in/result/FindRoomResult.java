package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;

/**
 * The retrieved room, including its creation, last update, and deletion timestamps.
 *
 * @param roomId the room identifier
 * @param code the room code
 * @param name the room name
 * @param description the room description
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
public record FindRoomResult(
        RoomId roomId,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    /**
     * Creates a result snapshot from the room.
     *
     * @param room the found room
     * @return the result
     * @throws NullPointerException if {@code room} is null
     */
    public static FindRoomResult from(Room room) {
        return new FindRoomResult(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                room.getDeletedAt());
    }
}
