package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ReserveSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveScheduleStatus;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ReservedScheduleRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservedScheduleJpaAdapter implements ReserveSchedulePort {
    private final ReservedScheduleRepository repository;

    /** {@inheritDoc} */
    @Override
    public ReserveScheduleStatus reserve(ReservationId reservationId, Set<ScheduleId> scheduleIds) {
        var ids = scheduleIds.stream().map(ScheduleId::getValue).toList();

        var insertedCount = repository.insertFromSchedules(reservationId.getValue(), ids);

        if (insertedCount != ids.size()) return ReserveScheduleStatus.FAILED;
        return ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED;
    }
}
