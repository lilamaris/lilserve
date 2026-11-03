package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The driving port that deletes a seat once. */
public interface DeleteSeatUseCase {

    /**
     * Marks the seat as deleted and moves {@code updatedAt} to {@code deletedAt}.
     *
     * @param command the delete request, must not be null
     * @return the deleted seat
     * @throws java.util.NoSuchElementException if the seat does not exist or is deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateSeatResult delete(@NotNull @Valid DeleteSeatCommand command);
}
