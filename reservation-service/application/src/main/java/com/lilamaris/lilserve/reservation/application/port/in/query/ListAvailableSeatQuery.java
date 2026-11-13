package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

/**
 * Requests seats available for selected recurring slots on one service date.
 *
 * @param roomId the owning room, not null when validated
 * @param targetDate the service date interpreted in the application timezone
 * @param targetSchedules the selected slot identifiers; an empty selection yields no seats
 */
public record ListAvailableSeatQuery(
        @NotNull RoomId roomId,
        @NotNull LocalDate targetDate,
        @NotNull Set<@NotNull ScheduleId> targetSchedules) {
    /**
     * Takes an immutable snapshot of the selected slot identifiers.
     *
     * @param roomId the owning room identifier
     * @param targetDate the requested service date
     * @param targetSchedules the selected slot identifiers
     * @throws NullPointerException if the selection or any selected identifier is null
     */
    public ListAvailableSeatQuery {
        targetSchedules = Set.copyOf(targetSchedules);
    }

    /**
     * Creates a seat availability query, validated at the use case boundary.
     *
     * @param roomId the owning room identifier
     * @param targetDate the requested service date
     * @param targetSchedules the selected slot identifiers
     * @return the query with an immutable slot selection
     */
    public static ListAvailableSeatQuery of(
            RoomId roomId, LocalDate targetDate, Set<ScheduleId> targetSchedules) {
        return new ListAvailableSeatQuery(roomId, targetDate, targetSchedules);
    }
}
