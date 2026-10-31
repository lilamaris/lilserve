package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * A request to change a seat's code. The code constraints mirror the rules {@link Seat} enforces.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param seatId the seat to change, must not be null when validated
 * @param code the new code, must be non-blank and within {@link Seat#MAX_CODE_LENGTH}
 */
public record UpdateSeatCodeCommand(
        @NotNull SeatId seatId, @NotBlank @Size(max = Seat.MAX_CODE_LENGTH) String code) {

    /**
     * Creates a command from the seat identifier and the new code. Code constraints are checked by
     * Bean Validation, not here.
     *
     * @param id the seat identifier
     * @param code the new code
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static UpdateSeatCodeCommand of(UUID id, String code) {
        var seatId = SeatId.of(id);
        return new UpdateSeatCodeCommand(seatId, code);
    }
}
