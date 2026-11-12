package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record FindScheduleQuery(@NotNull RoomId roomId, @NotNull Instant now) {
    /**
     * Creates a room schedule query.
     *
     * @param roomId the room identifier
     * @param now the instant used to determine active reservation intake blackouts
     * @return the query, validated at the use case boundary
     */
    public static FindScheduleQuery of(RoomId roomId, Instant now) {
        return new FindScheduleQuery(roomId, now);
    }
}
