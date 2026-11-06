package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Changes room codes. */
public interface UpdateRoomCodeUseCase {

    /**
     * Changes the room code and records {@code updatedAt}.
     *
     * @param command the update request, must not be null
     * @return the room with the new code
     * @throws java.util.NoSuchElementException if the room does not exist or is deleted
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     current clock time is before the creation time or the last update time
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code command} is null
     */
    UpdateRoomResult updateCode(@NotNull @Valid UpdateRoomCodeCommand command);
}
