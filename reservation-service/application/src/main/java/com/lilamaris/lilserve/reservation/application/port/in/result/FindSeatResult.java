package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;

/**
 * The retrieved seat, including its creation, last update, and deletion timestamps.
 *
 * @param seatId the seat identifier
 * @param roomId the room the seat belongs to
 * @param code the seat code
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
public record FindSeatResult(
        SeatId seatId,
        RoomId roomId,
        String code,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
    /**
     * Creates a result snapshot from the seat.
     *
     * @param seat the found seat
     * @return the result
     * @throws NullPointerException if {@code seat} is null
     */
    public static FindSeatResult from(Seat seat) {
        return new FindSeatResult(
                seat.getId(),
                seat.getRoomId(),
                seat.getCode(),
                seat.getCreatedAt(),
                seat.getUpdatedAt(),
                seat.getDeletedAt());
    }
}
