package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A request to create a seat in an existing, non-deleted room. The code constraints mirror the
 * rules enforced by {@link Seat}. Room existence is checked by the service.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room the seat belongs to, must not be null when validated
 * @param code the seat code, must be non-blank and within {@link Seat#MAX_CODE_LENGTH}
 */
public record CreateSeatCommand(
        @NotNull RoomId roomId, @NotBlank @Size(max = Seat.MAX_CODE_LENGTH) String code) {

    /**
     * Creates a command from the room identifier and the seat code. Required fields and code
     * constraints are checked by Bean Validation, not here.
     *
     * @param roomId the room the seat belongs to
     * @param code the seat code
     * @return the command
     */
    public static CreateSeatCommand of(RoomId roomId, String code) {
        return new CreateSeatCommand(roomId, code);
    }
}
