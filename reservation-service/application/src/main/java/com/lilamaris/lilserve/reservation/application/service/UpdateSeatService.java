package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
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
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateSeatService
        implements UpdateSeatCodeUseCase, DeleteSeatUseCase, RestoreSeatUseCase {
    private final SeatSavePort seatSavePort;
    private final SeatFindPort seatFindPort;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Loads a non-deleted seat, changes its code using the current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult updateCode(@NotNull @Valid UpdateSeatCodeCommand command) {
        var seat =
                seatFindPort
                        .findNotDeletedById(command.seatId())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.updateCode(command.code(), now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads a non-deleted seat, marks it as deleted at the current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult delete(@NotNull @Valid DeleteSeatCommand command) {
        var seat =
                seatFindPort
                        .findNotDeletedById(command.seatId())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.delete(now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads a deleted seat, clears its deletion at the current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateSeatResult restore(@NotNull @Valid RestoreSeatCommand command) {
        var seat =
                seatFindPort
                        .findDeletedById(command.seatId())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        var now = clock.instant();

        seat.restore(now);

        seatSavePort.save(seat);

        return UpdateSeatResult.from(seat);
    }
}
