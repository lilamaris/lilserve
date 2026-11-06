package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Changes seat codes. */
public interface UpdateSeatCodeUseCase {

    /**
     * Changes the seat code and records {@code updatedAt}.
     *
     * @param command the update request, must not be null
     * @return the seat with the new code
     * @throws java.util.NoSuchElementException if the seat does not exist or is deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateSeatResult updateCode(@NotNull @Valid UpdateSeatCodeCommand command);
}
