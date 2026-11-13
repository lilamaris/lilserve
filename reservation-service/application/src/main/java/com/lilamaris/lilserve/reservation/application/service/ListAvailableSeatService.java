package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.ListAvailableSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListAvailableSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.ListAvailableSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.AvailableSeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Lists available seats, returning no seats while any selected slot has an intake blackout. */
@Service
@Validated
@RequiredArgsConstructor
public class ListAvailableSeatService implements ListAvailableSeatUseCase {
    private final RoomFindPort roomFindPort;
    private final AvailableSeatFindPort availableSeatFindPort;
    private final BlackoutScheduleFindPort blackoutScheduleFindPort;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Evaluates intake blackouts at the current clock instant, independently of the requested
     * service date. Returns an empty list for an empty slot selection or an active blackout. This
     * lookup does not lock resources; reservation creation rechecks eligibility in its transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public ListAvailableSeatResult list(ListAvailableSeatQuery query) {
        var room =
                roomFindPort
                        .findNotDeletedById(query.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        if (query.targetSchedules().isEmpty())
            return new ListAvailableSeatResult(room.getId(), query.targetDate(), List.of());

        if (blackoutScheduleFindPort.existsActiveByScheduleIdsAt(
                query.targetSchedules(), clock.instant()))
            return new ListAvailableSeatResult(room.getId(), query.targetDate(), List.of());

        var seats =
                availableSeatFindPort.findAvailableSeat(
                        query.roomId(),
                        query.targetDate(),
                        query.targetSchedules(),
                        clock.getZone());

        return new ListAvailableSeatResult(
                room.getId(), query.targetDate(), seats.stream().map(Seat::getId).toList());
    }
}
