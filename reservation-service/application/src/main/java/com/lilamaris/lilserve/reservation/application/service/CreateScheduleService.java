package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import java.time.Clock;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CreateScheduleService implements CreateScheduleUseCase {
    private final ScheduleSavePort scheduleSavePort;
    private final RoomFindPort roomFindPort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CreateScheduleResult create(CreateScheduleCommand command) {
        var room =
                roomFindPort
                        .findNotDeletedById(command.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();
        var scheduleId = uuidGenerator.generate();
        var schedule =
                Schedule.of(
                        scheduleId,
                        room.getId().getValue(),
                        command.dayOfWeek(),
                        command.range().startAt(),
                        command.range().endAt(),
                        now);
        scheduleSavePort.save(schedule);
        return CreateScheduleResult.from(schedule);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CreateSchedulesResult create(CreateSchedulesCommand command) {
        var room =
                roomFindPort
                        .findNotDeletedById(command.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        var schedules = new ArrayList<Schedule>();
        for (var dayEntry : command.schedules().entrySet()) {
            var day = dayEntry.getKey();
            for (var entry : dayEntry.getValue()) {
                var scheduleId = uuidGenerator.generate();
                var schedule =
                        Schedule.of(
                                scheduleId,
                                room.getId().getValue(),
                                day,
                                entry.startAt(),
                                entry.endAt(),
                                now);
                schedules.add(schedule);
            }
        }

        scheduleSavePort.saveAll(schedules);
        return CreateSchedulesResult.from(schedules);
    }
}
