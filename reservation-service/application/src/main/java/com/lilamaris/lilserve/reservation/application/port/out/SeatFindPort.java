package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.util.Optional;

/**
 * Finds seats by identifier and deletion state within the caller's transaction.
 *
 * <p>Implementations return {@link Optional#empty()} when no seat matches and never return {@code
 * null}. The caller decides how to handle a missing seat.
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

    /**
     * Finds a page of non-deleted seats belonging to one room.
     *
     * @param roomId the owning room
     * @param pageRequest the requested page
     * @return the page ordered by code and identifier, with the room's active seat total
     */
    OffsetPage<Seat> findNotDeletedByRoomId(RoomId roomId, OffsetPageRequest pageRequest);
}
