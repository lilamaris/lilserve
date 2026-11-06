package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Restores deleted rooms. */
public interface RestoreRoomUseCase {

    /**
     * Clears the deletion of a deleted room and records the restore time in {@code updatedAt}.
     *
     * @param command the restore request, must not be null
     * @return the restored room
     * @throws java.util.NoSuchElementException if the room does not exist or is not deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateRoomResult restore(@NotNull @Valid RestoreRoomCommand command);
}
