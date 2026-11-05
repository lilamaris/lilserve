package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Instant;

public interface ReleaseSchedulePort {
    /**
     * Releases every active allocation of the reservation using one cancellation instant. Already
     * released allocations retain their cancellation times. The caller owns the transaction and
     * must cancel the reservation header in that same transaction. Persistence failures propagate
     * to the caller so the entire cancellation can be rolled back.
     *
     * @param reservationId the reservation identifier, not null
     * @param releasedAt the cancellation instant, at or after the allocations' creation times
     */
    void release(ReservationId reservationId, Instant releasedAt);
}
