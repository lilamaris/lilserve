package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface DeleteBlackoutScheduleUseCase {
    void delete(@NotNull @Valid DeleteBlackoutScheduleCommand command);
}
