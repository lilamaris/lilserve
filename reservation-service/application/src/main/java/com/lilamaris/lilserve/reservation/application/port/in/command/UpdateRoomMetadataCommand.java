package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * A request to change a room's name and description. The value constraints mirror the rules {@link
 * Room} enforces on {@code name} and {@code description}.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room to change, must not be null when validated
 * @param name the new name, must be non-blank and within {@link Room#MAX_NAME_LENGTH}
 * @param description the new description, must not be null when validated, within {@link
 *     Room#MAX_DESCRIPTION_LENGTH}, blank allowed
 */
public record UpdateRoomMetadataCommand(
        @NotNull RoomId roomId,
        @NotBlank @Size(max = Room.MAX_NAME_LENGTH) String name,
        @NotNull @Size(max = Room.MAX_DESCRIPTION_LENGTH) String description) {

    /**
     * Creates a command from the room identifier and the new metadata. The identifier must be
     * non-null; name and description constraints are checked by Bean Validation, not here.
     *
     * @param id the room identifier
     * @param name the new name
     * @param description the new description, blank allowed
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static UpdateRoomMetadataCommand of(UUID id, String name, String description) {
        var roomId = RoomId.of(id);
        return new UpdateRoomMetadataCommand(roomId, name, description);
    }
}
