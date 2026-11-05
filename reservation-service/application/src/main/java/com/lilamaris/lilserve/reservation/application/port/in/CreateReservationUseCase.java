package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateReservationUseCase {
    /**
     * Creates a reservation and all requested allocations in one transaction.
     *
     * @param command the reservation request
     * @return the created reservation after every requested slot has been secured
     */
    CreateReservationResult create(@NotNull @Valid CreateReservationCommand command);
}
