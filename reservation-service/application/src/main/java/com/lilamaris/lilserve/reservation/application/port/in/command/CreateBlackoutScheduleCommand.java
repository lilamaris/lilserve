package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBlackoutScheduleCommand(
        @NotNull RoomId roomId,
        @NotNull ScheduleId scheduleId,
        @NotNull InstantRange range,
        @NotBlank @Size(max = BlackoutSchedule.MAX_REASON_LENGTH) String reason) {
    public static CreateBlackoutScheduleCommand of(
            RoomId roomId, ScheduleId scheduleId, InstantRange range, String reason) {
        return new CreateBlackoutScheduleCommand(roomId, scheduleId, range, reason);
    }
}
