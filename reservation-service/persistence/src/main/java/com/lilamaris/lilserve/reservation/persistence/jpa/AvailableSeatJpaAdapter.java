package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.AvailableSeatFindPort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.SeatRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements available seat lookup using native SQL within the caller's transaction. */
@Component
@RequiredArgsConstructor
public class AvailableSeatJpaAdapter implements AvailableSeatFindPort {
    private final SeatRepository repository;

    /**
     * {@inheritDoc}
     *
     * <p>Unwraps identifiers to UUID values and the timezone to its name for native SQL binding.
     */
    @Override
    public List<Seat> findAvailableSeat(
            RoomId roomId, LocalDate targetDate, Set<ScheduleId> scheduleIds, ZoneId timeZone) {
        var ids = scheduleIds.stream().map(ScheduleId::getValue).toList();
        return repository.findAvailable(roomId.getValue(), targetDate, ids, timeZone.getId());
    }
}
