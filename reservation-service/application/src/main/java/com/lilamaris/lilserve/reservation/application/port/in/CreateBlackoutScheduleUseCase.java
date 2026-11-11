package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateBlackoutScheduleUseCase {
    void create(@NotNull @Valid CreateBlackoutScheduleCommand command);
}
