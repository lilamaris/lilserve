package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateScheduleUseCase {
    /**
     * Creates one recurring slot.
     *
     * @param command the slot request
     * @return the created slot
     */
    CreateScheduleResult create(@NotNull @Valid CreateScheduleCommand command);

    /**
     * Creates the requested recurring slots in one transaction.
     *
     * @param command the slot requests
     * @return the created slots, possibly empty
     */
    CreateSchedulesResult create(@NotNull @Valid CreateSchedulesCommand command);
}
