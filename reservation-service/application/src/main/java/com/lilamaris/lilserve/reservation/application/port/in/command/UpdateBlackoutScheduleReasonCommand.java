package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBlackoutScheduleReasonCommand(
        @NotNull BlackoutScheduleId blackoutScheduleId,
        @NotBlank @Size(max = BlackoutSchedule.MAX_REASON_LENGTH) String reason) {
    public static UpdateBlackoutScheduleReasonCommand of(
            BlackoutScheduleId blackoutScheduleId, String reason) {
        return new UpdateBlackoutScheduleReasonCommand(blackoutScheduleId, reason);
    }
}
