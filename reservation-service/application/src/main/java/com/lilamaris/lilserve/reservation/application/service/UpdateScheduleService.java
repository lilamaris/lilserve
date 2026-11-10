package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.RetireSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link UpdateScheduleUseCase} by retiring the original slot and inserting the
 * replacement slot in one transaction. The replacement keeps the parent room of the original slot,
 * receives a newly generated identifier, and shares one instant from {@link Clock} with the
 * retirement.
 *
 * <p>Replacements of the same original slot are serialized in lock acquisition order. After
 * waiting, each request validates the locked state: replacement requires an active slot. A retired
 * slot is rejected with {@link NoSuchElementException}, preserving the preceding replacement and
 * its retirement time. Unchecked failures roll back all changes in the transaction.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateScheduleService implements UpdateScheduleUseCase, DeleteScheduleUseCase {
    private final ScheduleLockFindPort scheduleLockFindPort;
    private final ScheduleSavePort scheduleSavePort;
    private final RetireSchedulePort retireSchedulePort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a slot, requires it to be active, then retires it and saves the
     * replacement slot with the requested weekday and time range under a newly generated
     * identifier. The replacement carries the room of the retired slot, and both changes are
     * recorded at the same instant.
     *
     * <p>Retirement flushes the managed original before the conditional bulk update runs. The
     * original is therefore already retired when that update executes; a zero update count does not
     * mean replacement failed. The exclusive lock protects the state validation, and both writes
     * roll back together if replacement fails. The original time definition and existing allocation
     * references remain unchanged.
     */
    @Override
    @Transactional
    public UpdateScheduleResult update(UpdateScheduleCommand command) {
        var oldSchedule =
                scheduleLockFindPort
                        .findByIdWithExclusiveLock(command.scheduleId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var now = clock.instant();

        oldSchedule.delete(now);

        var scheduleId = ScheduleId.of(uuidGenerator.generate());
        var newSchedule =
                Schedule.of(
                        scheduleId,
                        oldSchedule.getRoomId(),
                        command.dayOfWeek(),
                        command.range().startAt(),
                        command.range().endAt(),
                        now);

        retireSchedulePort.retire(oldSchedule.getId(), now);
        scheduleSavePort.save(newSchedule);
        return UpdateScheduleResult.from(newSchedule);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a slot, requires it to be active, then records its retirement time and
     * saves it without changing its time definition or existing allocation references.
     */
    @Override
    @Transactional
    public DeleteScheduleResult delete(ScheduleId scheduleId) {
        var schedule =
                scheduleLockFindPort
                        .findByIdWithExclusiveLock(scheduleId)
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var now = clock.instant();

        schedule.delete(now);

        scheduleSavePort.save(schedule);

        return DeleteScheduleResult.from(schedule);
    }
}
