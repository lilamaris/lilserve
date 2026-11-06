package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import jakarta.validation.constraints.NotNull;

/**
 * A request to cancel a whole reservation.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * @param reservationId the reservation identifier, must not be null when validated
 */
public record DeleteReservationCommand(@NotNull ReservationId reservationId) {
    /**
     * Creates a command from the reservation identifier. Required fields are checked by Bean
     * Validation.
     *
     * @param reservationId the reservation identifier
     * @return the cancellation request
     */
    public static DeleteReservationCommand of(ReservationId reservationId) {
        return new DeleteReservationCommand(reservationId);
    }
}
