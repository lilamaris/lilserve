package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The JSON body that creates a seat in a room. The code constraints mirror the rules {@link Seat}
 * enforces, so a request is rejected at the web boundary with 400 before the use case runs. The
 * room is not checked here: the use case loads it, so a room that does not exist is answered as
 * 404.
 *
 * @param roomId the room the seat belongs to, not null
 * @param code the seat code, non-blank and within {@link Seat#MAX_CODE_LENGTH}
 */
@Schema(
        description = "The body that creates a seat in a room.",
        example =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","code":"SEAT-01"}
                """)
public record CreateSeatRequest(
        @Schema(
                        description = "room the seat belongs to",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                UUID roomId,
        @Schema(
                        description = "seat code, unique across seats",
                        maxLength = Seat.MAX_CODE_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Seat.MAX_CODE_LENGTH)
                String code) {

    /**
     * Converts this body to a create command, wrapping the room UUID in {@link RoomId}. Bean
     * Validation checks request constraints at the web boundary.
     *
     * @return the create command
     * @throws NullPointerException if {@code roomId} is null
     */
    public CreateSeatCommand toCommand() {
        return CreateSeatCommand.of(RoomId.of(roomId), code);
    }
}
