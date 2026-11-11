package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import java.util.Optional;

public interface BlackoutScheduleLockFindPort {
    Optional<BlackoutSchedule> findByIdWithExclusiveLock(BlackoutScheduleId blackoutScheduleId);
}
