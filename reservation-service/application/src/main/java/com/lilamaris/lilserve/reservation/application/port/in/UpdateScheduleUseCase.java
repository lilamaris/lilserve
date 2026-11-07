package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Replaces the time definition of a recurring room slot. */
public interface UpdateScheduleUseCase {

    /**
     * Replaces a slot by retiring it and offering a new slot with the requested weekday and time
     * range in one transaction.
     *
     * <p>Slot definitions are append-only, so the times of the original slot are never edited in
     * place. The replacement is inserted as a new row under a new identifier and keeps the parent
     * room of the original slot. Retiring the original slot only stops it from being offered for
     * new allocations; allocations already made against it keep their reference to it.
     *
     * @param command the replacement request, must not be null
     * @return the replacement slot with its new identifier
     * @throws java.util.NoSuchElementException if the slot does not exist or is retired
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time of the slot
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateScheduleResult update(@NotNull @Valid UpdateScheduleCommand command);
}
