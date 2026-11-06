package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Creates reservations and their slot allocations. */
public interface CreateReservationUseCase {
    /**
     * Creates a reservation and all requested allocations in one transaction.
     *
     * @param command the reservation request, must not be null
     * @return the created reservation after every requested slot has been secured
     * @throws java.util.NoSuchElementException if the room or seat does not exist or is deleted
     * @throws IllegalArgumentException if the seat does not belong to the room
     * @throws IllegalStateException if not all requested slots can be allocated
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    CreateReservationResult create(@NotNull @Valid CreateReservationCommand command);
}
