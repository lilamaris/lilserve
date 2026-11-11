package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;

public interface BlackoutScheduleSavePort {
    void save(BlackoutSchedule blackoutSchedule);
}
