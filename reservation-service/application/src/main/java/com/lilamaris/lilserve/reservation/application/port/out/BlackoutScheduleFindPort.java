package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.util.Optional;

public interface BlackoutScheduleFindPort {
    Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId);
}
