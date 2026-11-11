package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Creates temporary reservation intake blackouts for recurring room slots. */
public interface CreateBlackoutScheduleUseCase {
    /**
     * Creates a blackout while protecting the active room and schedule in one transaction.
     *
     * <p>Locks the room in shared mode, then the schedule in exclusive mode. The schedule lock
     * serializes creation with reservation intake checks. Overlapping blackouts are allowed;
     * existing reservations are preserved. The database enforces room and schedule ownership.
     *
     * @param command the room, schedule, intake interval, and reason, not null
     * @throws java.util.NoSuchElementException if the room or schedule is missing or deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the command is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and the command is null
     */
    void create(@NotNull @Valid CreateBlackoutScheduleCommand command);
}
