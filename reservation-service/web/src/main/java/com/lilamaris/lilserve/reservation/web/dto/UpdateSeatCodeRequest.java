package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The JSON body that changes a seat's code. The code is unique in the database, and the web
 * exception handler maps a persistence integrity violation to HTTP 409.
 *
 * @param code the new code, non-blank and within {@link Seat#MAX_CODE_LENGTH}
 */
@Schema(description = "The body that changes a seat's code.", example = "{\"code\":\"SEAT-02\"}")
public record UpdateSeatCodeRequest(
        @Schema(
                        description = "new seat code, unique across seats",
                        maxLength = Seat.MAX_CODE_LENGTH,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = Seat.MAX_CODE_LENGTH)
                String code) {

    /**
     * Converts this body and the path identifier to the update command.
     *
     * @param seatId the seat whose code changes
     * @return the update command
     * @throws NullPointerException if {@code seatId} is null
     */
    public UpdateSeatCodeCommand toCommand(UUID seatId) {
        return UpdateSeatCodeCommand.of(SeatId.of(seatId), code);
    }
}
