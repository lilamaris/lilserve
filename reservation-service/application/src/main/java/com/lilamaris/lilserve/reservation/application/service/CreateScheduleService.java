package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link CreateScheduleUseCase} by locking the parent room and saving recurring slots in
 * one transaction.
 *
 * <p>Both creation paths acquire a shared row lock before checking that the room is active. The
 * lock is retained until the transaction ends, so room deletion must wait for creation to commit or
 * roll back. Identifiers are generated for each slot, and creation time comes from {@link Clock}.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateScheduleService implements CreateScheduleUseCase {
    private final ScheduleSavePort scheduleSavePort;
    private final RoomLockFindPort roomLockFindPort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Loads the parent room under a shared lock, rejects a deleted room, and saves the slot
     * within the same transaction. A runtime failure rolls back the transaction.
     */
    @Override
    @Transactional
    public CreateScheduleResult create(CreateScheduleCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithSharedLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();
        var scheduleId = ScheduleId.of(uuidGenerator.generate());
        var schedule =
                Schedule.of(
                        scheduleId,
                        room.getId(),
                        command.dayOfWeek(),
                        command.range().startAt(),
                        command.range().endAt(),
                        now);
        scheduleSavePort.save(schedule);
        return CreateScheduleResult.from(schedule);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Acquires one shared lock on the parent room and validates its active state before
     * preparing any slots. The lock covers the whole batch until transaction completion; a runtime
     * failure rolls back all slots, including changes already flushed to the database.
     */
    @Override
    @Transactional
    public CreateSchedulesResult create(CreateSchedulesCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithSharedLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        var schedules = new ArrayList<Schedule>();
        for (var dayEntry : command.schedules().entrySet()) {
            var day = dayEntry.getKey();
            for (var entry : dayEntry.getValue()) {
                var scheduleId = ScheduleId.of(uuidGenerator.generate());
                var schedule =
                        Schedule.of(
                                scheduleId, room.getId(), day, entry.startAt(), entry.endAt(), now);
                schedules.add(schedule);
            }
        }

        scheduleSavePort.saveAll(schedules);
        return CreateSchedulesResult.from(schedules);
    }
}
