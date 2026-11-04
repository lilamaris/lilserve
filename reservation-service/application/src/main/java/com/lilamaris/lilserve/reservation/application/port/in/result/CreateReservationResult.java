package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/** The created reservation header and all successfully requested slot identifiers. */
public record CreateReservationResult(
        ReservationId reservationId,
        RoomId roomId,
        SeatId seatId,
        Set<ScheduleId> scheduleIds,
        LocalDate reservationDate,
        String timeZone,
        Instant createdAt) {
    /**
     * Copies slot identifiers so callers cannot change the snapshot.
     *
     * @param reservationId the reservation identifier
     * @param roomId the room identifier
     * @param seatId the seat identifier
     * @param scheduleIds the successfully reserved slot identifiers
     * @param reservationDate the service date
     * @param timeZone the stored timezone
     * @param createdAt the creation instant
     */
    public CreateReservationResult {
        scheduleIds = Set.copyOf(scheduleIds);
    }

    /**
     * Maps the header after all requested slots have been reserved successfully.
     *
     * @param reservation the created header
     * @param scheduleIds the successfully reserved slot identifiers
     * @return the result
     */
    public static CreateReservationResult from(
            Reservation reservation, Set<ScheduleId> scheduleIds) {
        return new CreateReservationResult(
                reservation.getId(),
                reservation.getRoomId(),
                reservation.getSeatId(),
                scheduleIds,
                reservation.getReservationDate(),
                reservation.getTimeZone(),
                reservation.getCreatedAt());
    }
}
