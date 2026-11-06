package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Restores deleted seats. */
public interface RestoreSeatUseCase {

    /**
     * Clears the deletion of a deleted seat and records the restore time in {@code updatedAt}.
     *
     * @param command the restore request, must not be null
     * @return the restored seat
     * @throws java.util.NoSuchElementException if the seat does not exist or is not deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateSeatResult restore(@NotNull @Valid RestoreSeatCommand command);
}
