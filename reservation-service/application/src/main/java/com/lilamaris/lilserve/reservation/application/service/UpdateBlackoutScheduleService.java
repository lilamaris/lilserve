package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutSchedulePeriodUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutScheduleReasonUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteBlackoutScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateBlackoutScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import java.time.Clock;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Changes or soft-deletes active reservation intake blackouts in a transaction.
 *
 * <p>Period changes and deletion read only the immutable parent identifier before locking the
 * schedule and then the blackout exclusively. The final locked blackout determines whether the
 * request is still valid. Reason changes lock only the blackout. All mutation timestamps come from
 * {@link Clock} after the required locks are acquired; runtime failures roll back the change. Each
 * result reports the locked blackout as it was stored, so the committed state reaches the caller in
 * the same transaction that changed it.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateBlackoutScheduleService
        implements UpdateBlackoutScheduleReasonUseCase,
                UpdateBlackoutSchedulePeriodUseCase,
                DeleteBlackoutScheduleUseCase {
    private final BlackoutScheduleFindPort blackoutScheduleFindPort;
    private final BlackoutScheduleLockFindPort blackoutScheduleLockFindPort;
    private final ScheduleLockFindPort scheduleLockFindPort;
    private final BlackoutScheduleSavePort blackoutScheduleSavePort;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UpdateBlackoutScheduleResult updateReason(UpdateBlackoutScheduleReasonCommand command) {
        var blackoutSchedule =
                blackoutScheduleLockFindPort
                        .findByIdWithExclusiveLock(command.blackoutScheduleId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "blackout schedule does not exists."));

        var now = clock.instant();

        blackoutSchedule.updateReason(command.reason(), now);

        blackoutScheduleSavePort.save(blackoutSchedule);

        return UpdateBlackoutScheduleResult.from(blackoutSchedule);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UpdateBlackoutScheduleResult updateRange(UpdateBlackoutSchedulePeriodCommand command) {
        var scheduleId =
                blackoutScheduleFindPort
                        .findScheduleIdById(command.blackoutScheduleId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "blackout schedule does not exists."));

        scheduleLockFindPort
                .findByIdWithExclusiveLock(scheduleId)
                .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var blackoutSchedule =
                blackoutScheduleLockFindPort
                        .findByIdWithExclusiveLock(command.blackoutScheduleId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "blackout schedule does not exists."));

        var now = clock.instant();

        blackoutSchedule.updatePeriod(command.period().startsAt(), command.period().endsAt(), now);

        blackoutScheduleSavePort.save(blackoutSchedule);

        return UpdateBlackoutScheduleResult.from(blackoutSchedule);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DeleteBlackoutScheduleResult delete(DeleteBlackoutScheduleCommand command) {
        var scheduleId =
                blackoutScheduleFindPort
                        .findScheduleIdById(command.blackoutScheduleId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "blackout schedule does not exists."));

        scheduleLockFindPort
                .findByIdWithExclusiveLock(scheduleId)
                .orElseThrow(() -> new NoSuchElementException("schedule does not exists."));

        var blackoutSchedule =
                blackoutScheduleLockFindPort
                        .findByIdWithExclusiveLock(command.blackoutScheduleId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "blackout schedule does not exists."));

        var now = clock.instant();

        blackoutSchedule.delete(now);

        blackoutScheduleSavePort.save(blackoutSchedule);

        return DeleteBlackoutScheduleResult.from(blackoutSchedule);
    }
}
