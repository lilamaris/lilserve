package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.util.Optional;

/**
 * Finds a seat by identifier under a shared row lock within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when no seat matches and never return {@code
 * null}. The caller decides how to handle a missing seat.
 *
 * @see SeatFindPort
 * @see SeatSavePort
 */
public interface SeatLockFindPort {

    /**
     * Finds a seat by identifier and keeps a shared lock on its row until the caller's transaction
     * ends. Use this for operations that validate a seat before writing, such as reservation.
     *
     * <p>Requires an active transaction. The lock and the read are requested by one statement, so
     * the returned deletion state is the state this transaction protects. A deleted seat is
     * returned with that state, and a missing row is reported as an empty result.
     *
     * @param seatId the seat identifier, not null
     * @return the locked seat with its deletion state, or empty when no seat has this identifier
     * @throws NullPointerException if {@code seatId} is null
     */
    Optional<Seat> findLockedById(SeatId seatId);
}
