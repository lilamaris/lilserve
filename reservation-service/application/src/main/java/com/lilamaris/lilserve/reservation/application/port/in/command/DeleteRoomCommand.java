package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A request to delete a room. Only the target identifier is required.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room to delete, must not be null when validated
 */
public record DeleteRoomCommand(@NotNull RoomId roomId) {

    /**
     * Creates a command from the room identifier.
     *
     * @param id the room identifier
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static DeleteRoomCommand of(UUID id) {
        var roomId = RoomId.of(id);
        return new DeleteRoomCommand(roomId);
    }
}
