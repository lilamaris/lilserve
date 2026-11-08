package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Cancels reservations and releases their slot allocations. */
public interface DeleteReservationUseCase {
    /**
     * Cancels an active reservation and releases all of its allocations in one transaction. Only
     * reservations with at least one active allocation whose earliest start is strictly after the
     * current time can be cancelled. The header and allocations share one cancellation instant.
     *
     * <p>Concurrent cancellation requests cannot overwrite the first committed cancellation time. A
     * request that cannot release every allocation is rejected and its changes are rolled back.
     *
     * @param command the cancellation request, must not be null
     * @return the cancelled reservation identifier and cancellation time
     * @throws java.util.NoSuchElementException if the reservation does not exist, is already
     *     cancelled, has no active allocations, or has already reached its earliest start time
     * @throws IllegalStateException if no allocations or only some allocations are released, such
     *     as when another request has already committed their cancellation after this request's
     *     eligibility check
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    DeleteReservationResult delete(@NotNull @Valid DeleteReservationCommand command);
}
