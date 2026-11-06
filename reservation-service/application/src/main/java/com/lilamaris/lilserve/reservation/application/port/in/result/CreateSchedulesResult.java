package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import java.util.Collection;
import java.util.List;

/** The created slots. The list is immutable and has no guaranteed weekday ordering. */
public record CreateSchedulesResult(List<CreateScheduleResult> schedules) {
    /**
     * Copies the result list so callers cannot change the snapshot.
     *
     * @param schedules the created slot results
     */
    public CreateSchedulesResult {
        schedules = List.copyOf(schedules);
    }

    /**
     * Creates immutable result snapshots from the recurring slots.
     *
     * @param schedules the created slots
     * @return the result, with an empty list when no slots were created
     */
    public static CreateSchedulesResult from(Collection<Schedule> schedules) {
        return new CreateSchedulesResult(
                schedules.stream().map(CreateScheduleResult::from).toList());
    }
}
