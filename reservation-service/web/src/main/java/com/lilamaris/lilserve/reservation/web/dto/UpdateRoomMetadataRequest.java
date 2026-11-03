package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The JSON body that changes a room's name and description. The constraints mirror the rules {@link
 * Room} enforces, so a request is rejected at the web boundary with 400 before the use case runs.
 *
 * @param name the new name, non-blank and within {@link Room#MAX_NAME_LENGTH}
 * @param description the new description, not null, within {@link Room#MAX_DESCRIPTION_LENGTH},
 *     blank allowed
 */
@Schema(
        description = "The body that changes a room's name and description.",
        example =
                """
                {"name":"small hall","description":"capacity 40"}
                """)
public record UpdateRoomMetadataRequest(
        @Schema(
                        description = "new room name",
                        maxLength = Room.MAX_NAME_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Room.MAX_NAME_LENGTH)
                String name,
        @Schema(
                        description = "new room description, blank allowed",
                        maxLength = Room.MAX_DESCRIPTION_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                @Size(max = Room.MAX_DESCRIPTION_LENGTH)
                String description) {

    /**
     * Converts this body and the path identifier to the update command.
     *
     * @param roomId the room whose metadata changes
     * @return the update command
     * @throws NullPointerException if {@code roomId} is null
     */
    public UpdateRoomMetadataCommand toCommand(UUID roomId) {
        return UpdateRoomMetadataCommand.of(roomId, name, description);
    }
}
