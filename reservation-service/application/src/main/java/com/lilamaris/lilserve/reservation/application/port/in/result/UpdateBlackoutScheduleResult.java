package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;

/**
 * A blackout after a change. Every change moves {@code updatedAt} forward, and deletion is recorded
 * in {@code deletedAt}.
 *
 * @param blackoutScheduleId the blackout identifier
 * @param roomId the room owning the blocked slot
 * @param scheduleId the blocked recurring slot
 * @param reason the blackout reason
 * @param startsAt the inclusive intake start instant
 * @param endsAt the exclusive intake end instant
 * @param createdAt the creation time
 * @param updatedAt the last update time
 * @param deletedAt the deletion time, null when not deleted
 */
public record UpdateBlackoutScheduleResult(
        BlackoutScheduleId blackoutScheduleId,
        RoomId roomId,
        ScheduleId scheduleId,
        String reason,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    /**
     * Creates a result snapshot from the changed blackout.
     *
     * @param blackoutSchedule the changed blackout
     * @return the result
     * @throws NullPointerException if {@code blackoutSchedule} is null
     */
    public static UpdateBlackoutScheduleResult from(BlackoutSchedule blackoutSchedule) {
        return new UpdateBlackoutScheduleResult(
                blackoutSchedule.getId(),
                blackoutSchedule.getRoomId(),
                blackoutSchedule.getScheduleId(),
                blackoutSchedule.getReason(),
                blackoutSchedule.getStartsAt(),
                blackoutSchedule.getEndsAt(),
                blackoutSchedule.getCreatedAt(),
                blackoutSchedule.getUpdatedAt(),
                blackoutSchedule.getDeletedAt());
    }
}
