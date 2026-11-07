package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

/**
 * The replacement recurring slot created by a schedule update.
 *
 * @param scheduleId the new slot identifier
 * @param roomId the room retained from the original slot
 * @param dayOfWeek the requested ISO weekday
 * @param startAt the local start time
 * @param endAt the local end time
 * @param createdAt the replacement creation time
 */
public record UpdateScheduleResult(
        ScheduleId scheduleId,
        RoomId roomId,
        DayOfWeek dayOfWeek,
        LocalTime startAt,
        LocalTime endAt,
        Instant createdAt) {

    /**
     * Creates a result snapshot from the replacement slot.
     *
     * @param schedule the replacement slot
     * @return the result
     * @throws NullPointerException if {@code schedule} is null
     */
    public static UpdateScheduleResult from(Schedule schedule) {
        return new UpdateScheduleResult(
                schedule.getId(),
                schedule.getRoomId(),
                schedule.getDayOfWeek(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getCreatedAt());
    }
}
