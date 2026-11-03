package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A request to restore a deleted room. Only the target identifier is required.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room to restore, must not be null when validated
 */
public record RestoreRoomCommand(@NotNull RoomId roomId) {

    /**
     * Creates a command from the room identifier.
     *
     * @param id the room identifier
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static RestoreRoomCommand of(UUID id) {
        var roomId = RoomId.of(id);
        return new RestoreRoomCommand(roomId);
    }
}
