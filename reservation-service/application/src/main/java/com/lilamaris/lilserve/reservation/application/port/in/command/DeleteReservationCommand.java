package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import jakarta.validation.constraints.NotNull;

/**
 * A request to cancel a whole reservation.
 *
 * @param reservationId the reservation identifier, not null
 */
public record DeleteReservationCommand(@NotNull ReservationId reservationId) {
    /**
     * Creates a cancellation request. Required fields are validated at the use case boundary.
     *
     * @param reservationId the reservation identifier
     * @return the cancellation request
     */
    public static DeleteReservationCommand of(ReservationId reservationId) {
        return new DeleteReservationCommand(reservationId);
    }
}
