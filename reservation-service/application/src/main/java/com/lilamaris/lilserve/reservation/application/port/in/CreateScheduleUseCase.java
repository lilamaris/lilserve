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
     * Creates one recurring slot.
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
     * Creates the requested recurring slots in one transaction.
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
