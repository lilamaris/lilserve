package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Instant;

/** Releases slot allocations within the caller's transaction. */
public interface ReleaseSchedulePort {
    /**
     * Releases every active allocation of the reservation using one cancellation instant. Already
     * released allocations retain their cancellation times. The caller owns the transaction and
     * must cancel the reservation header in that same transaction.
     *
     * <p>The result describes allocations changed by this request. Returning a status does not roll
     * back changes: the caller must roll back the header and any released allocations unless the
     * result is {@link ReleaseScheduleStatus#ALL_RELEASED}. Persistence failures propagate to the
     * caller so the entire cancellation can be rolled back.
     *
     * @param reservationId the reservation identifier, not null
     * @param releasedAt the cancellation instant, at or after the allocations' creation times
     * @return whether all, some, or none of the reservation's allocations were released
     */
    ReleaseScheduleStatus release(ReservationId reservationId, Instant releasedAt);
}
