package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.BlackoutScheduleRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlackoutScheduleJpaAdapter
        implements BlackoutScheduleSavePort,
                BlackoutScheduleLockFindPort,
                BlackoutScheduleFindPort {
    private final BlackoutScheduleRepository repository;

    @Override
    public Optional<BlackoutSchedule> findByIdWithExclusiveLock(
            BlackoutScheduleId blackoutScheduleId) {
        return repository.findByIdWithExclusiveLock(blackoutScheduleId);
    }

    @Override
    public void save(BlackoutSchedule blackoutSchedule) {
        repository.save(blackoutSchedule);
    }

    @Override
    public Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId) {
        return repository.findScheduleIdById(blackoutScheduleId);
    }
}
