package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.ReleaseSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import java.time.Clock;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link DeleteReservationUseCase} by loading an eligible reservation, cancelling its
 * header, and releasing its allocations in one transaction. All changes use the same instant from
 * {@link Clock}.
 */
@Service
@Validated
@RequiredArgsConstructor
public class DeleteReservationService implements DeleteReservationUseCase {
    private final ReservationFindPort reservationFindPort;
    private final ReservationSavePort reservationSavePort;
    private final ReleaseSchedulePort releaseSchedulePort;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DeleteReservationResult delete(DeleteReservationCommand command) {
        var now = clock.instant();

        var reservation =
                reservationFindPort
                        .findNotStartedById(command.reservationId(), now)
                        .orElseThrow(
                                () -> new NoSuchElementException("reservation does not exists."));

        reservation.delete(now);
        reservationSavePort.save(reservation);
        releaseSchedulePort.release(reservation.getId(), now);
        return DeleteReservationResult.from(reservation);
    }
}
