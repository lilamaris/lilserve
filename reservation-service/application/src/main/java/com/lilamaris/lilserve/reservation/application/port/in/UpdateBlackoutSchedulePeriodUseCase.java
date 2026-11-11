package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Changes the interval during which a slot rejects new reservation intake. */
public interface UpdateBlackoutSchedulePeriodUseCase {
    /**
     * Replaces an active blackout's intake interval in one transaction.
     *
     * <p>Locks the parent schedule before the blackout, both exclusively, so reservations decide
     * against either the preceding or the committed replacement interval. The parent schedule's
     * retirement state is not checked. Existing reservations are preserved.
     *
     * @param command the blackout identifier and replacement interval, not null
     * @throws java.util.NoSuchElementException if the parent schedule is missing or the blackout is
     *     missing or deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and the command is null
     */
    void updateRange(@NotNull @Valid UpdateBlackoutSchedulePeriodCommand command);
}
