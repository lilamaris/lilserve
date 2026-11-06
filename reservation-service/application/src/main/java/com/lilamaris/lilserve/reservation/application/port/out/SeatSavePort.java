package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Seat;

/** Persists seats within the caller's transaction. */
public interface SeatSavePort {

    /**
     * Stores the seat, inserting it when it is new.
     *
     * @param seat the seat to store, not null
     * @throws NullPointerException if {@code seat} is null
     */
    void save(Seat seat);
}
