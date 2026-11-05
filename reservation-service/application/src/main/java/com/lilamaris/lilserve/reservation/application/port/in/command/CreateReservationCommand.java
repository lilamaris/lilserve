package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

/**
 * A request for one seat and service date; service entry validates required fields and selection.
 */
public record CreateReservationCommand(
        @NotNull RoomId roomId,
        @NotNull SeatId seatId,
        @NotEmpty Set<@NotNull ScheduleId> scheduleIds,
        @NotNull LocalDate reservationDate) {
    /**
     * Takes an immutable snapshot of the selected slot identifiers.
     *
     * @param roomId the requested room, required at the service boundary
     * @param seatId the requested seat, required at the service boundary
     * @param scheduleIds selected slots; null collections and elements are rejected immediately
     * @param reservationDate the service date, required at the service boundary
     */
    public CreateReservationCommand {
        scheduleIds = Set.copyOf(scheduleIds);
    }

    /**
     * Creates a request with an immutable slot selection.
     *
     * @param roomId the requested room
     * @param seatId the requested seat
     * @param scheduleIds selected slots; must be non-empty at the service boundary
     * @param reservationDate the service date
     * @return the request
     */
    public static CreateReservationCommand of(
            RoomId roomId, SeatId seatId, Set<ScheduleId> scheduleIds, LocalDate reservationDate) {
        return new CreateReservationCommand(roomId, seatId, scheduleIds, reservationDate);
    }
}
