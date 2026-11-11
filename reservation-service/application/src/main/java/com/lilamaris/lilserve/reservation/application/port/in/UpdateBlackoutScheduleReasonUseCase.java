package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UpdateBlackoutScheduleReasonUseCase {
    void updateReason(@NotNull @Valid UpdateBlackoutScheduleReasonCommand command);
}
