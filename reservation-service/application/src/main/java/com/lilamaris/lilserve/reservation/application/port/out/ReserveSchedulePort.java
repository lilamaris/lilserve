package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Set;

/** Creates slot allocations within the caller's transaction. */
public interface ReserveSchedulePort {
    /**
     * Attempts to allocate all selected slots to an active reservation header.
     *
     * <p>The caller supplies a non-empty set without null elements and owns the transaction. The
     * result describes whether all, some, or none of the requested slots were inserted. Returning a
     * status does not roll back changes: the caller must roll back the header and any inserted
     * allocations unless the result is {@link ReserveScheduleStatus#ALL_RESERVED}. Implementations
     * propagate unexpected persistence failures rather than reporting them as ordinary allocation
     * outcomes. They do not commit independently.
     *
     * @param reservationId the reservation header identifier, not null
     * @param scheduleIds the requested slot identifiers, non-empty and without null elements
     * @return whether all, some, or none of the requested slots were allocated
     */
    ReserveScheduleStatus reserve(ReservationId reservationId, Set<ScheduleId> scheduleIds);
}
