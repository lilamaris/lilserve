package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ReleaseSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReleaseScheduleStatus;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveScheduleStatus;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ReservedScheduleRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements slot allocation persistence ports using Spring Data JPA within the caller's
 * transaction.
 */
@Component
@RequiredArgsConstructor
public class ReservedScheduleJpaAdapter implements ReserveSchedulePort, ReleaseSchedulePort {
    private final ReservedScheduleRepository repository;

    /**
     * {@inheritDoc}
     *
     * <p>Compares the number of allocations inserted by the repository with the number of requested
     * slots to distinguish complete, partial, and empty allocation outcomes. Reports the outcome
     * without committing or rolling back; the caller decides whether the transaction may commit.
     */
    @Override
    public ReserveScheduleStatus reserve(ReservationId reservationId, Set<ScheduleId> scheduleIds) {
        var ids = scheduleIds.stream().map(ScheduleId::getValue).toList();

        var insertedCount = repository.insertFromSchedules(reservationId.getValue(), ids);

        if (insertedCount == 0) return ReserveScheduleStatus.NONE_RESERVED;
        if (insertedCount != ids.size()) return ReserveScheduleStatus.PARTIAL_RESERVED;
        return ReserveScheduleStatus.ALL_RESERVED;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Flushes pending entity changes, including the reservation header, before releasing
     * allocations. Compares the total allocation count, including previously released rows, with
     * the rows changed by this request. Reports the outcome without committing or rolling back; the
     * caller decides whether the transaction may commit.
     */
    @Override
    public ReleaseScheduleStatus release(ReservationId reservationId, Instant releasedAt) {
        repository.flush();
        var row = repository.markAsDeletedByReservationId(reservationId.getValue(), releasedAt);

        if (row.updatedCount() == 0) return ReleaseScheduleStatus.NONE_RELEASED;
        return row.totalCount() == row.updatedCount()
                ? ReleaseScheduleStatus.ALL_RELEASED
                : ReleaseScheduleStatus.PARTIAL_RELEASED;
    }
}
