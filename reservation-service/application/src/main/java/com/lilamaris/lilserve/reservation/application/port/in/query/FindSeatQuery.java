package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A request to find one seat.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this query
 * is explicitly validated or passed to a use case with method validation active.
 *
 * @param seatId the seat to find, must not be null when validated
 */
public record FindSeatQuery(@NotNull SeatId seatId) {

    /**
     * Creates a query from the seat identifier.
     *
     * @param id the seat identifier
     * @return the query
     * @throws NullPointerException if {@code id} is null
     */
    public static FindSeatQuery of(UUID id) {
        var seatId = SeatId.of(id);
        return new FindSeatQuery(seatId);
    }
}
