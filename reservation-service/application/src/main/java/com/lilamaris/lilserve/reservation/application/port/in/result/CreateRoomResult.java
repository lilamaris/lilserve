package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;

/**
 * The created room. A new room has no update and no deletion timestamp, so those are not carried.
 *
 * @param roomId the room identifier
 * @param code the room code
 * @param name the room name
 * @param description the room description
 * @param createdAt the creation time
 */
public record CreateRoomResult(
        RoomId roomId, String code, String name, String description, Instant createdAt) {

    /**
     * Creates a result snapshot from the room.
     *
     * @param room the created room
     * @return the result
     * @throws NullPointerException if {@code room} is null
     */
    public static CreateRoomResult from(Room room) {
        return new CreateRoomResult(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.getCreatedAt());
    }
}
