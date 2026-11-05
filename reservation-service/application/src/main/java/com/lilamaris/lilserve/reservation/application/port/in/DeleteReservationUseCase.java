package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface DeleteReservationUseCase {
    /**
     * Cancels an active reservation and releases all of its allocations in one transaction. Only
     * reservations with at least one active allocation whose earliest start is strictly after the
     * current time can be cancelled. The header and allocations share one cancellation instant.
     *
     * @param command the cancellation request
     * @return the cancelled reservation identifier and cancellation time
     * @throws java.util.NoSuchElementException if the reservation does not exist, is already
     *     cancelled, has no active allocations, or has already reached its earliest start time
     */
    DeleteReservationResult delete(@NotNull @Valid DeleteReservationCommand command);
}
