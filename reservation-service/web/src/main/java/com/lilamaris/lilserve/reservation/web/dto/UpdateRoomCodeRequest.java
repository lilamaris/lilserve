package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The JSON body that changes a room's code. The code is unique in the database, so a duplicate is
 * reported as 409 by the persistence layer.
 *
 * @param code the new code, non-blank and within {@link Room#MAX_CODE_LENGTH}
 */
@Schema(description = "The body that changes a room's code.", example = "{\"code\":\"ROOM-02\"}")
public record UpdateRoomCodeRequest(
        @Schema(
                        description = "new room code, unique across rooms",
                        maxLength = Room.MAX_CODE_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Room.MAX_CODE_LENGTH)
                String code) {

    /**
     * Converts this body and the path identifier to the update command.
     *
     * @param roomId the room whose code changes
     * @return the update command
     * @throws NullPointerException if {@code roomId} is null
     */
    public UpdateRoomCodeCommand toCommand(UUID roomId) {
        return UpdateRoomCodeCommand.of(roomId, code);
    }
}
