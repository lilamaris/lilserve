package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.BlackoutScheduleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements blackout read, exclusive-lock, and save ports using JPA.
 *
 * <p>Participates in the caller's transaction. Scalar parent lookup does not load a blackout
 * entity; usage overlap queries unwrap identifiers and timezone names for native SQL binding.
 */
@Component
@RequiredArgsConstructor
public class BlackoutScheduleJpaAdapter
        implements BlackoutScheduleSavePort,
                BlackoutScheduleLockFindPort,
                BlackoutScheduleFindPort {
    private final BlackoutScheduleRepository repository;

    /** {@inheritDoc} */
    @Override
    public Optional<BlackoutSchedule> findByIdWithExclusiveLock(
            BlackoutScheduleId blackoutScheduleId) {
        return repository.findByIdWithExclusiveLock(blackoutScheduleId);
    }

    /** {@inheritDoc} */
    @Override
    public void save(BlackoutSchedule blackoutSchedule) {
        repository.save(blackoutSchedule);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsOverlappingUsage(
            RoomId roomId, Set<ScheduleId> scheduleIds, LocalDate serviceDate, ZoneId timeZone) {
        var ids = scheduleIds.stream().map(ScheduleId::getValue).toList();
        return repository.existsOverlappingUsage(
                roomId.getValue(), ids, serviceDate, timeZone.getId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<RoomId> findRoomIdById(BlackoutScheduleId blackoutScheduleId) {
        return repository.findRoomIdById(blackoutScheduleId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ScheduleId> findScheduleIdById(BlackoutScheduleId blackoutScheduleId) {
        return repository.findScheduleIdById(blackoutScheduleId);
    }

    /** {@inheritDoc} */
    @Override
    public List<BlackoutSchedule> findNotDeletedByRoomIdAt(RoomId roomId, Instant now) {
        return repository.findActiveByRoomIdAt(roomId, now);
    }
}
