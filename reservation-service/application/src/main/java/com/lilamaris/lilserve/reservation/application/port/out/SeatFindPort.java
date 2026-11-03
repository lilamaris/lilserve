package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.util.Optional;

/**
 * The driven port that reads one seat, implemented by the persistence adapter.
 *
 * <p>Deletion is soft: a deleted seat keeps its row and carries its {@code deletedAt}, so reading
 * with deleted seats and reading without them are separate operations. Callers pick the operation
 * that matches their rule, for example restore needs the deleted seat itself and a code change must
 * reject it.
 *
 * <p>Implementations return {@link Optional#empty()} when no seat matches and never return {@code
 * null}; a missing seat is not an error at this port. What a missing seat means is the caller's
 * decision, and the port takes no transaction boundary of its own: it runs inside the caller's
 * transaction.
 *
 * @see SeatSavePort
 */
public interface SeatFindPort {

    /**
     * Finds a seat by identifier, deleted seats included.
     *
     * @param seatId the seat identifier, not null
     * @return the seat with its deletion state, or empty when no seat has this identifier
     * @throws NullPointerException if {@code seatId} is null
     */
    Optional<Seat> findById(SeatId seatId);

    /**
     * Finds a seat by identifier when it is not deleted. Use this for operations that must reject a
     * deleted seat, such as code change and delete.
     *
     * @param seatId the seat identifier, not null
     * @return the seat, or empty when it does not exist or is deleted
     * @throws NullPointerException if {@code seatId} is null
     */
    Optional<Seat> findNotDeletedById(SeatId seatId);

    /**
     * Finds a seat by identifier when it is deleted. Use this for restore, which needs the deleted
     * seat itself.
     *
     * @param seatId the seat identifier, not null
     * @return the deleted seat, or empty when it does not exist or is not deleted
     * @throws NullPointerException if {@code seatId} is null
     */
    Optional<Seat> findDeletedById(SeatId seatId);
}
