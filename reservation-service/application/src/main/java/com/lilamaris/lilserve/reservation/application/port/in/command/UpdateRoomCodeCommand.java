package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

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
     * Creates a command from the room identifier and the new code. Code constraints are checked by
     * Bean Validation, not here.
     *
     * @param id the room identifier
     * @param code the new code
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static UpdateRoomCodeCommand of(UUID id, String code) {
        var roomId = RoomId.of(id);
        return new UpdateRoomCodeCommand(roomId, code);
    }
}
