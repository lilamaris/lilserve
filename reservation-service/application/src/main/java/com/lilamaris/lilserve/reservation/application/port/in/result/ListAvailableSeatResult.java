package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.LocalDate;
import java.util.List;

/**
 * Seat availability observed at lookup time, without guaranteeing a subsequent reservation.
 *
 * @param roomId the owning room identifier
 * @param targetDate the requested service date
 * @param availableSeatIds available seat identifiers ordered by seat code and identifier
 */
public record ListAvailableSeatResult(
        RoomId roomId, LocalDate targetDate, List<SeatId> availableSeatIds) {}
