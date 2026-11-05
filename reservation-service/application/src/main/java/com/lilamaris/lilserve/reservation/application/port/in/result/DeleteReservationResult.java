package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Instant;

/**
 * The cancelled reservation identifier and the cancellation time shared with its allocations.
 *
 * @param reservationId the cancelled reservation identifier
 * @param deletedAt the cancellation instant
 */
public record DeleteReservationResult(ReservationId reservationId, Instant deletedAt) {
    /**
     * Maps the cancelled header after all of its active allocations have been released.
     *
     * @param reservation the cancelled reservation header
     * @return the cancellation result
     * @throws NullPointerException if {@code reservation} is null
     */
    public static DeleteReservationResult from(Reservation reservation) {
        return new DeleteReservationResult(reservation.getId(), reservation.getDeletedAt());
    }
}
