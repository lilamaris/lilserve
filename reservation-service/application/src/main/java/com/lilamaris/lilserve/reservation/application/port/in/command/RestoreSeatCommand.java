package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A request to restore a deleted seat. Only the target identifier is required.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param seatId the seat to restore, must not be null when validated
 */
public record RestoreSeatCommand(@NotNull SeatId seatId) {

    /**
     * Creates a command from the seat identifier.
     *
     * @param id the seat identifier
     * @return the command
     * @throws NullPointerException if {@code id} is null
     */
    public static RestoreSeatCommand of(UUID id) {
        var seatId = SeatId.of(id);
        return new RestoreSeatCommand(seatId);
    }
}
