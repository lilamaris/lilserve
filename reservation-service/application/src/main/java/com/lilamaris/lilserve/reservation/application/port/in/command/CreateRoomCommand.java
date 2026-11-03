package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Room;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A request to create a room. The value constraints mirror the rules enforced by {@link Room}.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param code the room code, must be non-blank and within {@link Room#MAX_CODE_LENGTH}
 * @param name the room name, must be non-blank and within {@link Room#MAX_NAME_LENGTH}
 * @param description the room description, must not be null when validated, within {@link
 *     Room#MAX_DESCRIPTION_LENGTH}, blank allowed
 */
public record CreateRoomCommand(
        @NotBlank @Size(max = Room.MAX_CODE_LENGTH) String code,
        @NotBlank @Size(max = Room.MAX_NAME_LENGTH) String name,
        @NotNull @Size(max = Room.MAX_DESCRIPTION_LENGTH) String description) {

    /**
     * Creates a command with the given values. Value constraints are checked by Bean Validation,
     * not here.
     *
     * @param code the room code
     * @param name the room name
     * @param description the room description, blank allowed
     * @return the command
     */
    public static CreateRoomCommand of(String code, String name, String description) {
        return new CreateRoomCommand(code, name, description);
    }
}
