package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;

/**
 * A seat after a change. Every change moves {@code updatedAt} forward, and deletion is recorded in
 * {@code deletedAt}.
 *
 * @param seatId the seat identifier
 * @param roomId the room the seat belongs to
 * @param code the seat code
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
public record UpdateSeatResult(
        SeatId seatId,
        RoomId roomId,
        String code,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    /**
     * Maps a seat to the result.
     *
     * @param seat the changed seat
     * @return the result
     * @throws NullPointerException if {@code seat} is null
     */
    public static UpdateSeatResult from(Seat seat) {
        return new UpdateSeatResult(
                seat.getId(),
                seat.getRoomId(),
                seat.getCode(),
                seat.getCreatedAt(),
                seat.getUpdatedAt(),
                seat.getDeletedAt());
    }
}
