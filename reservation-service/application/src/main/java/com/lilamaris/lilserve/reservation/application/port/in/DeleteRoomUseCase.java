package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The driving port that deletes a room once. */
public interface DeleteRoomUseCase {

    /**
     * Marks the room as deleted and moves {@code updatedAt} to {@code deletedAt}.
     *
     * @param command the delete request, must not be null
     * @return the deleted room
     * @throws java.util.NoSuchElementException if the room does not exist or is deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateRoomResult delete(@NotNull @Valid DeleteRoomCommand command);
}
