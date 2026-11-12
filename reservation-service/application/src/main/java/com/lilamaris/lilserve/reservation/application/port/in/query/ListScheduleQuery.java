package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * A request to list the recurring slots of one room.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this query
 * is explicitly validated or passed to a use case with method validation active.
 *
 * @param roomId the room whose slots to list, must not be null when validated
 * @param now the intake instant whose active blackouts are reported, must not be null when
 *     validated
 */
public record ListScheduleQuery(@NotNull RoomId roomId, @NotNull Instant now) {
    /**
     * Creates a room slot list query.
     *
     * @param roomId the room identifier
     * @param now the instant used to determine active reservation intake blackouts
     * @return the query, validated at the use case boundary
     */
    public static ListScheduleQuery of(RoomId roomId, Instant now) {
        return new ListScheduleQuery(roomId, now);
    }
}
