package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

/** The created recurring slot, without a retirement timestamp. */
public record CreateScheduleResult(
        ScheduleId scheduleId,
        RoomId roomId,
        DayOfWeek dayOfWeek,
        LocalTime startAt,
        LocalTime endAt,
        Instant createdAt) {
    /**
     * Maps a created slot to its result snapshot.
     *
     * @param schedule the created slot
     * @return the result
     */
    public static CreateScheduleResult from(Schedule schedule) {
        return new CreateScheduleResult(
                schedule.getId(),
                schedule.getRoomId(),
                schedule.getDayOfWeek(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getCreatedAt());
    }
}
