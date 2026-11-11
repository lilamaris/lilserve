package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotNull;

public record UpdateBlackoutSchedulePeriodCommand(
        @NotNull BlackoutScheduleId blackoutScheduleId, @NotNull InstantRange period) {
    public static UpdateBlackoutSchedulePeriodCommand of(
            BlackoutScheduleId blackoutScheduleId, InstantRange period) {
        return new UpdateBlackoutSchedulePeriodCommand(blackoutScheduleId, period);
    }
}
