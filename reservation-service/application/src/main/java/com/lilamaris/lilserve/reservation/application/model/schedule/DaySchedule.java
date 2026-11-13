package com.lilamaris.lilserve.reservation.application.model.schedule;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.util.List;

/**
 * One recurring slot of a room with the room usage blackouts active at the queried instant.
 *
 * @param scheduleId the slot identifier
 * @param dayOfWeek the ISO weekday the slot repeats on
 * @param period the slot's local time interval
 * @param blackouts the blackouts active at the queried instant, empty when usage is open
 */
public record DaySchedule(
        ScheduleId scheduleId,
        DayOfWeek dayOfWeek,
        LocalTimeRange period,
        List<BlackoutScheduleSummary> blackouts) {
    /**
     * Projects a recurring slot and the room restrictions active at the requested instant into
     * immutable summaries.
     *
     * @param schedule the recurring slot
     * @param blackout the room restrictions, empty when the room is available
     * @return the slot summary with an immutable blackout list
     */
    public static DaySchedule from(Schedule schedule, List<BlackoutSchedule> blackout) {
        return new DaySchedule(
                schedule.getId(),
                schedule.getDayOfWeek(),
                LocalTimeRange.of(schedule.getStartAt(), schedule.getEndAt()),
                blackout.stream().map(BlackoutScheduleSummary::from).toList());
    }
}
