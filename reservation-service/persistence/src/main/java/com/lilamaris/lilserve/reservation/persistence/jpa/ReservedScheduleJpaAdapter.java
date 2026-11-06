package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ReleaseSchedulePort;
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

    /** {@inheritDoc} */
    @Override
    public ReserveScheduleStatus reserve(ReservationId reservationId, Set<ScheduleId> scheduleIds) {
        var ids = scheduleIds.stream().map(ScheduleId::getValue).toList();

        var insertedCount = repository.insertFromSchedules(reservationId.getValue(), ids);

        if (insertedCount != ids.size()) return ReserveScheduleStatus.FAILED;
        return ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED;
    }

    /** {@inheritDoc} */
    @Override
    public void release(ReservationId reservationId, Instant releasedAt) {
        repository.markAsDeletedByReservationId(reservationId.getValue(), releasedAt);
    }
}
