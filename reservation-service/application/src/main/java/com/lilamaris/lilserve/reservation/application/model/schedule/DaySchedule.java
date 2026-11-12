package com.lilamaris.lilserve.reservation.application.model.schedule;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.util.List;

public record DaySchedule(
        ScheduleId scheduleId,
        DayOfWeek dayOfWeek,
        LocalTimeRange period,
        List<BlackoutScheduleSummary> blackouts) {
    /**
     * Projects a recurring slot and all its active blackouts into immutable summaries.
     *
     * @param schedule the recurring slot
     * @param blackout its active blackouts, empty when intake is open
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
