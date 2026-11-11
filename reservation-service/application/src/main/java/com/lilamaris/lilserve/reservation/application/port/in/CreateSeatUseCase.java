package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Creates seats in existing rooms. */
public interface CreateSeatUseCase {

    /**
     * Creates a seat in an active parent room within one transaction.
     *
     * <p>Acquires a shared lock on the parent room before validating its active state and retains
     * the lock until the transaction ends. A preceding room deletion that commits causes rejection;
     * if that deletion rolls back, creation can proceed. A runtime failure rolls back creation.
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
