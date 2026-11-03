package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The JSON body that creates a room. The constraints mirror the rules {@link Room} enforces, so a
 * request is rejected at the web boundary with 400 before the use case runs.
 *
 * @param code the room code, non-blank and within {@link Room#MAX_CODE_LENGTH}
 * @param name the room name, non-blank and within {@link Room#MAX_NAME_LENGTH}
 * @param description the room description, not null, within {@link Room#MAX_DESCRIPTION_LENGTH},
 *     blank allowed
 */
@Schema(
        description = "The body that creates a room.",
        example =
                """
                {"code":"ROOM-01","name":"main hall","description":"capacity 100"}
                """)
public record CreateRoomRequest(
        @Schema(
                        description = "room code, unique across rooms",
                        maxLength = Room.MAX_CODE_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Room.MAX_CODE_LENGTH)
                String code,
        @Schema(
                        description = "room name",
                        maxLength = Room.MAX_NAME_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Room.MAX_NAME_LENGTH)
                String name,
        @Schema(
                        description = "room description, blank allowed",
                        maxLength = Room.MAX_DESCRIPTION_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                @Size(max = Room.MAX_DESCRIPTION_LENGTH)
                String description) {

    /**
     * Converts this body to the create command. Value constraints are checked by Bean Validation at
     * the web boundary, not here.
     *
     * @return the create command with the same values
     */
    public CreateRoomCommand toCommand() {
        return CreateRoomCommand.of(code, name, description);
    }
}
