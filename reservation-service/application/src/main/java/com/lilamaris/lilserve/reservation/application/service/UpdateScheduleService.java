package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.RetireSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
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
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateScheduleService implements UpdateScheduleUseCase, DeleteScheduleUseCase {
    private final ScheduleFindPort scheduleFindPort;
    private final ScheduleSavePort scheduleSavePort;
    private final RetireSchedulePort retireSchedulePort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Loads a non-deleted slot, retires it, and saves the replacement slot with the requested
     * weekday and time range under a newly generated identifier. The replacement carries the room
     * of the retired slot, and both changes are recorded at the same instant.
     */
    @Override
    @Transactional
    public UpdateScheduleResult update(UpdateScheduleCommand command) {
        var oldSchedule =
                scheduleFindPort
                        .findNotDeletedById(command.scheduleId())
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DeleteScheduleResult delete(ScheduleId scheduleId) {
        var schedule =
                scheduleFindPort
                        .findNotDeletedById(scheduleId)
                        .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var now = clock.instant();

        schedule.delete(now);

        scheduleSavePort.save(schedule);

        return DeleteScheduleResult.from(schedule);
    }
}
