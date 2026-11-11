package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Creates recurring room slots. */
public interface CreateScheduleUseCase {
    /**
     * Creates one recurring slot in a transaction that protects the active parent room.
     *
     * <p>Acquires a shared lock on the parent room before validating its active state and retains
     * the lock until the transaction ends. A preceding room deletion that commits causes rejection;
     * if that deletion rolls back, creation can proceed. A runtime failure rolls back creation.
     *
     * @param command the slot request, must not be null
     * @return the created slot
     * @throws java.util.NoSuchElementException if the parent room does not exist or is deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    CreateScheduleResult create(@NotNull @Valid CreateScheduleCommand command);

    /**
     * Creates the requested recurring slots atomically in one transaction.
     *
     * <p>Acquires a shared lock on the parent room before validating its active state and retains
     * the lock until the entire batch commits or rolls back. A preceding room deletion that commits
     * causes rejection; if that deletion rolls back, creation can proceed. A runtime failure leaves
     * none of the requested slots persisted.
     *
     * @param command the slot requests, must not be null
     * @return the created slots, possibly empty
     * @throws java.util.NoSuchElementException if the parent room does not exist or is deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    CreateSchedulesResult create(@NotNull @Valid CreateSchedulesCommand command);
}
