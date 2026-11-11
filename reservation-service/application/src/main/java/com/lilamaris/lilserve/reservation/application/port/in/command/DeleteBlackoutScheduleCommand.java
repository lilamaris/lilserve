package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotNull;

public record DeleteBlackoutScheduleCommand(@NotNull BlackoutScheduleId blackoutScheduleId) {
    public static DeleteBlackoutScheduleCommand of(BlackoutScheduleId blackoutScheduleId) {
        return new DeleteBlackoutScheduleCommand(blackoutScheduleId);
    }
}
