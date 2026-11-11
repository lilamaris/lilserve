package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CreateBlackoutScheduleService implements CreateBlackoutScheduleUseCase {
    private final BlackoutScheduleSavePort blackoutScheduleSavePort;
    private final RoomLockFindPort roomLockFindPort;
    private final ScheduleLockFindPort scheduleLockFindPort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public void create(CreateBlackoutScheduleCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithSharedLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var schedule =
                scheduleLockFindPort
                        .findByIdWithExclusiveLock(command.scheduleId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var now = clock.instant();
        var blackoutScheduleId = BlackoutScheduleId.of(uuidGenerator.generate());
        var blackoutSchedule =
                BlackoutSchedule.of(
                        blackoutScheduleId,
                        room.getId(),
                        schedule.getId(),
                        command.reason(),
                        command.range().startsAt(),
                        command.range().endsAt(),
                        now);

        blackoutScheduleSavePort.save(blackoutSchedule);
    }
}
