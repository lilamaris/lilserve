package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface BlackoutScheduleFindPort {
    boolean existsActiveByScheduleIdsAt(Set<ScheduleId> scheduleIds, Instant now);

    Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId);
}
