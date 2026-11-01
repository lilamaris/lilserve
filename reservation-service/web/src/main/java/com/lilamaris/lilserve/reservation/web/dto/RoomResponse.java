package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.CreateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * The room as the web layer returns it. A room created in this request has no update and no
 * deletion timestamp, so those fields are null.
 *
 * @param roomId the room identifier
 * @param code the room code
 * @param name the room name
 * @param description the room description
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
@Schema(
        description =
                "The room as the web layer returns it. A room created in this request has no update"
                        + " and no deletion timestamp, so those fields are null.",
        example =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","code":"ROOM-01","name":"main"
                + " hall","description":"capacity 100","createdAt":"2026-01-01T00:00:00Z"}
                """)
public record RoomResponse(
        @Schema(
                        description = "room identifier",
                        type = "string",
                        format = "uuid",
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(description = "room code", accessMode = Schema.AccessMode.READ_ONLY) String code,
        @Schema(description = "room name", accessMode = Schema.AccessMode.READ_ONLY) String name,
        @Schema(description = "room description", accessMode = Schema.AccessMode.READ_ONLY)
                String description,
        @Schema(
                        description = "creation time",
                        type = "string",
                        format = "date-time",
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant createdAt,
        @Schema(
                        description = "last update time, null when never updated",
                        type = "string",
                        format = "date-time",
                        nullable = true,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant updatedAt,
        @Schema(
                        description = "deletion time, null when not deleted",
                        type = "string",
                        format = "date-time",
                        nullable = true,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant deletedAt) {

    /**
     * Maps a create result: the new room has no update and no deletion timestamp.
     *
     * @param result the created room
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static RoomResponse from(CreateRoomResult result) {
        return new RoomResponse(
                result.roomId().getValue(),
                result.code(),
                result.name(),
                result.description(),
                result.createdAt(),
                null,
                null);
    }

    /**
     * Maps a find result with the room's change history.
     *
     * @param result the found room
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static RoomResponse from(FindRoomResult result) {
        return new RoomResponse(
                result.roomId().getValue(),
                result.code(),
                result.name(),
                result.description(),
                result.createdAt(),
                result.updatedAt(),
                result.deletedAt());
    }

    /**
     * Maps a change result with the room's change history.
     *
     * @param result the changed room
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static RoomResponse from(UpdateRoomResult result) {
        return new RoomResponse(
                result.roomId().getValue(),
                result.code(),
                result.name(),
                result.description(),
                result.createdAt(),
                result.updatedAt(),
                result.deletedAt());
    }
}
