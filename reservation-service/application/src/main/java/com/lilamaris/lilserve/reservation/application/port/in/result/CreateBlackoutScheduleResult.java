package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;

/**
 * The created room usage blackout, without an update or deletion timestamp.
 *
 * @param blackoutScheduleId the created blackout identifier
 * @param roomId the restricted room
 * @param scheduleId the registration slot; the restriction applies to every slot in the room
 * @param reason the blackout reason
 * @param startsAt the inclusive usage start instant
 * @param endsAt the exclusive usage end instant
 * @param createdAt the creation time
 */
public record CreateBlackoutScheduleResult(
        BlackoutScheduleId blackoutScheduleId,
        RoomId roomId,
        ScheduleId scheduleId,
        String reason,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt) {

    /**
     * Creates a result snapshot from the created blackout.
     *
     * @param blackoutSchedule the created blackout
     * @return the result
     * @throws NullPointerException if {@code blackoutSchedule} is null
     */
    public static CreateBlackoutScheduleResult from(BlackoutSchedule blackoutSchedule) {
        return new CreateBlackoutScheduleResult(
                blackoutSchedule.getId(),
                blackoutSchedule.getRoomId(),
                blackoutSchedule.getScheduleId(),
                blackoutSchedule.getReason(),
                blackoutSchedule.getStartsAt(),
                blackoutSchedule.getEndsAt(),
                blackoutSchedule.getCreatedAt());
    }
}
