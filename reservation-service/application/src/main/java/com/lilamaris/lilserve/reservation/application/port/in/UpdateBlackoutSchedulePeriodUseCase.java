package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateBlackoutScheduleResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Changes the interval during which a room is physically unavailable. */
public interface UpdateBlackoutSchedulePeriodUseCase {
    /**
     * Replaces an active blackout's usage interval in one transaction.
     *
     * <p>Locks the owning room, registration schedule, then blackout exclusively, so reservations
     * decide against either the preceding or the committed replacement interval. The parent
     * schedule's retirement state is not checked. Existing reservations are preserved.
     *
     * @param command the blackout identifier and replacement interval, not null
     * @return the blackout with the replaced interval
     * @throws java.util.NoSuchElementException if the parent schedule is missing or the blackout is
     *     missing or deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and the command is null
     */
    UpdateBlackoutScheduleResult updateRange(
            @NotNull @Valid UpdateBlackoutSchedulePeriodCommand command);
}
