package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/** Reads seats without active allocations overlapping any selected slot on the service date. */
public interface AvailableSeatFindPort {
    /**
     * Finds non-deleted seats with no occupied time overlapping the requested slots.
     *
     * <p>The caller validates the room, slot selection, and usage blackouts. Slot times are
     * interpreted in the same timezone as reservation creation. Existing allocations retain their
     * stored instants, including allocations referring to retired slots. Intervals are half-open.
     *
     * @param roomId the owning room identifier
     * @param targetDate the requested service date
     * @param scheduleIds non-empty selected slot identifiers belonging to the room and weekday
     * @param timeZone the application timezone used by reservation creation
     * @return available seats ordered by code and identifier
     */
    List<Seat> findAvailableSeat(
            RoomId roomId, LocalDate targetDate, Set<ScheduleId> scheduleIds, ZoneId timeZone);
}
