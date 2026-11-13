package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateBlackoutScheduleResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Changes the explanatory reason of a room usage blackout. */
public interface UpdateBlackoutScheduleReasonUseCase {
    /**
     * Updates an active blackout's reason and update time in one transaction.
     *
     * <p>Locks only the blackout exclusively: changing its reason does not affect usage decisions.
     * A blackout deleted by a preceding transaction is rejected after the lock is acquired.
     *
     * @param command the blackout identifier and replacement reason, not null
     * @return the blackout with the new reason
     * @throws java.util.NoSuchElementException if the blackout is missing or deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and the command is null
     */
    UpdateBlackoutScheduleResult updateReason(
            @NotNull @Valid UpdateBlackoutScheduleReasonCommand command);
}
