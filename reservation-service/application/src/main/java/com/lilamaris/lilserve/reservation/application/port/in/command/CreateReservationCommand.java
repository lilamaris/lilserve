package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

/**
 * A request to reserve one seat on one service date.
 *
 * <p>Record construction does not run Bean Validation. The constraints are checked when this
 * command is explicitly validated or passed to a use case with method validation active.
 *
 * <p>The slot identifiers are copied at construction; null collections and elements are rejected
 * immediately.
 *
 * @param roomId the requested room, must not be null when validated
 * @param seatId the requested seat, must not be null when validated
 * @param scheduleIds the selected slots, non-empty when validated and without null elements
 * @param reservationDate the service date, must not be null when validated
 */
public record CreateReservationCommand(
        @NotNull RoomId roomId,
        @NotNull SeatId seatId,
        @NotEmpty Set<@NotNull ScheduleId> scheduleIds,
        @NotNull LocalDate reservationDate) {
    /**
     * Takes an immutable snapshot of the selected slot identifiers.
     *
     * @param roomId the requested room, must not be null when validated
     * @param seatId the requested seat, must not be null when validated
     * @param scheduleIds selected slots; null collections and elements are rejected immediately
     * @param reservationDate the service date, must not be null when validated
     */
    public CreateReservationCommand {
        scheduleIds = Set.copyOf(scheduleIds);
    }

    /**
     * Creates a command with an immutable slot selection.
     *
     * @param roomId the requested room
     * @param seatId the requested seat
     * @param scheduleIds selected slots; must be non-empty when validated
     * @param reservationDate the service date
     * @return the command
     * @throws NullPointerException if {@code scheduleIds} or any element is null
     */
    public static CreateReservationCommand of(
            RoomId roomId, SeatId seatId, Set<ScheduleId> scheduleIds, LocalDate reservationDate) {
        return new CreateReservationCommand(roomId, seatId, scheduleIds, reservationDate);
    }
}
