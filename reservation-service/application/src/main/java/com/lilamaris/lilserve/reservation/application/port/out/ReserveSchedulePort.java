package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Set;

/** Writes slot allocations within the reservation workflow's transaction. */
public interface ReserveSchedulePort {
    /**
     * Attempts to allocate all selected slots to an active reservation header.
     *
     * <p>The caller supplies a non-empty set without null elements and owns the transaction.
     * Success means every requested slot was inserted. A failed result can still leave some
     * allocations inserted in that transaction; the caller must roll back the whole reservation.
     * Implementations propagate unexpected persistence failures rather than reporting them as
     * ordinary allocation conflicts. They do not commit independently.
     *
     * @param reservationId the reservation header identifier, not null
     * @param scheduleIds the requested slot identifiers, non-empty and without null elements
     * @return success when all requested slots were allocated, otherwise failure
     */
    ReserveScheduleStatus reserve(ReservationId reservationId, Set<ScheduleId> scheduleIds);
}
