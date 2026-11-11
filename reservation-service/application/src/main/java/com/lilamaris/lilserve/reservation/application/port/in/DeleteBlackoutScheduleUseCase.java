package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Soft-deletes reservation intake blackouts. */
public interface DeleteBlackoutScheduleUseCase {
    /**
     * Deletes an active blackout and records its deletion and update time atomically.
     *
     * <p>Locks the parent schedule before the blackout, both exclusively, and rejects a blackout
     * that was deleted while the request waited. Existing reservations are preserved.
     *
     * @param command the blackout identifier, not null
     * @throws java.util.NoSuchElementException if the parent schedule is missing or the blackout is
     *     missing or deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and the command is null
     */
    void delete(@NotNull @Valid DeleteBlackoutScheduleCommand command);
}
