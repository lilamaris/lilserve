package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ScheduleRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements recurring slot persistence ports using Spring Data JPA within the caller's
 * transaction.
 */
@Component
@RequiredArgsConstructor
public class ScheduleJpaAdapter implements ScheduleSavePort {
    private final ScheduleRepository repository;

    /** {@inheritDoc} */
    @Override
    public void save(Schedule schedule) {
        repository.save(schedule);
    }

    /** {@inheritDoc} */
    @Override
    public void saveAll(Collection<Schedule> schedules) {
        repository.saveAll(schedules);
    }
}
