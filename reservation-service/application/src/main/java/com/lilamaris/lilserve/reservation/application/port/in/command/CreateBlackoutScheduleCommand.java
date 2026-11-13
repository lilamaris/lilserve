package com.lilamaris.lilserve.reservation.application.port.in.command;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requests a room-wide physical usage restriction with a registration slot.
 *
 * <p>Construction and the factory retain the supplied values without running Bean Validation.
 * Required fields and text constraints are checked by explicit validation or a validated use case.
 * A non-null {@link InstantRange} validates its own bounds when constructed.
 *
 * @param roomId the room offering the slot, required when validated
 * @param scheduleId the recurring registration slot; the restriction applies to the whole room,
 *     required when validated
 * @param range the absolute usage interval, inclusive at the start and exclusive at the end,
 *     required when validated
 * @param reason the non-blank explanation, at most the blackout reason length limit, required when
 *     validated
 */
public record CreateBlackoutScheduleCommand(
        @NotNull RoomId roomId,
        @NotNull ScheduleId scheduleId,
        @NotNull InstantRange range,
        @NotBlank @Size(max = BlackoutSchedule.MAX_REASON_LENGTH) String reason) {
    /**
     * Creates a command without running Bean Validation.
     *
     * @param roomId the room offering the slot
     * @param scheduleId the recurring registration slot; the restriction applies to the whole room
     * @param range the absolute usage interval, inclusive at the start and exclusive at the end
     * @param reason the non-blank explanation, at most the blackout reason length limit
     * @return the command
     */
    public static CreateBlackoutScheduleCommand of(
            RoomId roomId, ScheduleId scheduleId, InstantRange range, String reason) {
        return new CreateBlackoutScheduleCommand(roomId, scheduleId, range, reason);
    }
}
