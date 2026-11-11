package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UpdateBlackoutSchedulePeriodUseCase {
    void updateRange(@NotNull @Valid UpdateBlackoutSchedulePeriodCommand command);
}
