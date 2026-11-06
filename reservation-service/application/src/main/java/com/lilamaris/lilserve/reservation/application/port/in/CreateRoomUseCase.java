package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Creates rooms. */
public interface CreateRoomUseCase {

    /**
     * Creates a room from the command.
     *
     * @param command the create request, must not be null
     * @return the created room
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    CreateRoomResult create(@NotNull @Valid CreateRoomCommand command);
}
