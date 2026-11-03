package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The driving port that creates a seat in a room. */
public interface CreateSeatUseCase {

    /**
     * Creates a seat in an existing, non-deleted room from the command.
     *
     * @param command the create request, must not be null
     * @return the created seat
     * @throws java.util.NoSuchElementException if the parent room does not exist or is deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    CreateSeatResult create(@NotNull @Valid CreateSeatCommand command);
}
