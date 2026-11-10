package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.SeatLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements seat update, deletion, and restoration use cases. Each operation loads a seat in the
 * required deletion state, applies the change using {@link Clock}, and saves it through {@link
 * SeatSavePort} in one transaction.
 *
 * <p>Changes to the same seat are serialized in lock acquisition order. After waiting, each request
 * validates the locked state: modifications and deletion require an active seat, while restoration
 * requires a deleted seat. An incompatible state is rejected with {@link NoSuchElementException},
 * preserving the preceding change and its timestamps. Unchecked failures roll back all changes in
 * the transaction.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateSeatService
        implements UpdateSeatCodeUseCase, DeleteSeatUseCase, RestoreSeatUseCase {
    private final SeatLockFindPort seatLockFindPort;
    private final SeatSavePort seatSavePort;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a seat, requires it to be active, then changes its code using the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult updateCode(@NotNull @Valid UpdateSeatCodeCommand command) {
        var seat =
                seatLockFindPort
                        .findByIdWithExclusiveLock(command.seatId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.updateCode(command.code(), now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a seat, requires it to be active, then marks it as deleted at the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult delete(@NotNull @Valid DeleteSeatCommand command) {
        var seat =
                seatLockFindPort
                        .findByIdWithExclusiveLock(command.seatId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.delete(now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a seat, requires it to be deleted, then clears its deletion at the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult restore(@NotNull @Valid RestoreSeatCommand command) {
        var seat =
                seatLockFindPort
                        .findByIdWithExclusiveLock(command.seatId())
                        .filter(Seat::isDeleted)
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.restore(now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }
}
