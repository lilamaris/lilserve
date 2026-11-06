package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A request to change a room's code. The code constraints mirror the rules {@link Room} enforces.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room to change, must not be null when validated
 * @param code the new code, must be non-blank and within {@link Room#MAX_CODE_LENGTH}
 */
public record UpdateRoomCodeCommand(
        @NotNull RoomId roomId, @NotBlank @Size(max = Room.MAX_CODE_LENGTH) String code) {

    /**
     * Creates a command from the room identifier and the new code. Required fields and code
     * constraints are checked by Bean Validation, not here.
     *
     * @param roomId the room identifier
     * @param code the new code
     * @return the command
     */
    public static UpdateRoomCodeCommand of(RoomId roomId, String code) {
        return new UpdateRoomCodeCommand(roomId, code);
    }
}
