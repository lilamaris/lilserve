package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteScheduleResult;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;

/** Retires a recurring room slot. */
public interface DeleteScheduleUseCase {
    /**
     * Retires a slot so that it is no longer offered for new allocations.
     *
     * @param scheduleId the slot identifier
     * @return the retired slot identifier and its retirement time
     * @throws java.util.NoSuchElementException if the slot does not exist or is retired
     */
    DeleteScheduleResult delete(ScheduleId scheduleId);
}
