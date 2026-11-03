package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * A request to find one room.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this query
 * is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room to find, must not be null when validated
 */
public record FindRoomQuery(@NotNull RoomId roomId) {

    /**
     * Creates a query from the room identifier.
     *
     * @param id the room identifier
     * @return the query
     * @throws NullPointerException if {@code id} is null
     */
    public static FindRoomQuery of(UUID id) {
        var roomId = RoomId.of(id);
        return new FindRoomQuery(roomId);
    }
}
