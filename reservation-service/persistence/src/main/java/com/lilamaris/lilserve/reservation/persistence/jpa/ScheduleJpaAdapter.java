package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.RetireSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ScheduleRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements recurring slot persistence ports using Spring Data JPA within the caller's
 * transaction.
 *
 * <p>Retirement is applied as a bulk update, so a slot can be retired without being loaded first.
 */
@Component
@RequiredArgsConstructor
public class ScheduleJpaAdapter
        implements ScheduleFindPort, ScheduleSavePort, RetireSchedulePort, ScheduleLockFindPort {
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

    /**
     * {@inheritDoc}
     *
     * <p>Updates the matching row directly and leaves slots that are already retired untouched, so
     * the retirement time of a slot in the persistence context is not refreshed by this call.
     */
    @Override
    public void retire(ScheduleId scheduleId, Instant retiredAt) {
        repository.markAsDeletedByScheduleId(scheduleId.getValue(), retiredAt);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Schedule> findNotDeletedById(ScheduleId scheduleId) {
        return repository.findById(scheduleId).filter(schedule -> !schedule.isDeleted());
    }

    /** {@inheritDoc} */
    @Override
    public List<Schedule> findNotDeletedByRoomId(RoomId roomId) {
        return repository.findActiveByRoomId(roomId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Slots are loaded by a query that requests the shared lock and sorts by identifier, so
     * competing transactions acquire rows in the same order. A partial lock is reported as an empty
     * result.
     */
    @Override
    public Optional<Set<ScheduleId>> findAllByIdsWithSharedLock(Set<ScheduleId> scheduleIds) {
        var lockedIds = lockedIdentifiers(repository.findByIdsWithSharedLock(scheduleIds));
        if (!lockedIds.containsAll(scheduleIds)) {
            return Optional.empty();
        }
        return Optional.of(lockedIds);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Schedule> findByIdWithExclusiveLock(ScheduleId scheduleId) {
        return repository.findByIdWithExclusiveLock(scheduleId);
    }

    private Set<ScheduleId> lockedIdentifiers(List<Schedule> lockedSchedules) {
        return lockedSchedules.stream().map(Schedule::getId).collect(Collectors.toSet());
    }
}
