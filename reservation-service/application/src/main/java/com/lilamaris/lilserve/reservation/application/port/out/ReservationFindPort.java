package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Instant;
import java.util.Optional;

/** Finds reservations eligible for cancellation within the caller's transaction. */
public interface ReservationFindPort {
    /**
     * Finds an active reservation with active allocations that have not started yet.
     *
     * @param reservationId the reservation identifier, not null
     * @param now the comparison instant, not null
     * @return the reservation if its earliest active allocation starts strictly after {@code now};
     *     empty if absent, cancelled, without active allocations, or already started
     */
    Optional<Reservation> findNotStartedById(ReservationId reservationId, Instant now);
}
