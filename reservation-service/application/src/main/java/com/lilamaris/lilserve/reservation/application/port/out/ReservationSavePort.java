package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Reservation;

/** Persists reservation headers within the caller's transaction. */
public interface ReservationSavePort {
    /**
     * Saves a reservation header without requiring an immediate flush or commit. Allocations must
     * be saved in the same transaction; failure must roll back both.
     *
     * @param reservation the header to save, not null
     */
    void save(Reservation reservation);
}
