package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;

/**
 * The retired recurring slot identifier and its retirement time.
 *
 * @param scheduleId the retired slot identifier
 * @param deletedAt the retirement instant
 */
public record DeleteScheduleResult(ScheduleId scheduleId, Instant deletedAt) {
    /**
     * Creates a result snapshot from the retired slot.
     *
     * @param schedule the retired slot
     * @return the retirement result
     * @throws NullPointerException if {@code schedule} is null
     */
    public static DeleteScheduleResult from(Schedule schedule) {
        return new DeleteScheduleResult(schedule.getId(), schedule.getDeletedAt());
    }
}
