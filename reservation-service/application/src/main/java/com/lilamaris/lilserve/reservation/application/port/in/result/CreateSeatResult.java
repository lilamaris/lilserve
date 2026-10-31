package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;

/**
 * The created seat. A new seat has no update and no deletion timestamp, so those are not carried.
 *
 * @param seatId the seat identifier
 * @param roomId the room the seat belongs to
 * @param code the seat code
 * @param createdAt the creation time
 */
public record CreateSeatResult(SeatId seatId, RoomId roomId, String code, Instant createdAt) {

    /**
     * Maps a seat to the result.
     *
     * @param seat the created seat
     * @return the result
     * @throws NullPointerException if {@code seat} is null
     */
    public static CreateSeatResult from(Seat seat) {
        var roomId = RoomId.of(seat.getRoomId());
        return new CreateSeatResult(seat.getId(), roomId, seat.getCode(), seat.getCreatedAt());
    }
}
