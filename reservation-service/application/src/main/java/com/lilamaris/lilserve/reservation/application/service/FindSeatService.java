package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** Handles {@link FindSeatUseCase} by reading a non-deleted seat through {@link SeatFindPort}. */
@Service
@Validated
@RequiredArgsConstructor
public class FindSeatService implements FindSeatUseCase {
    private final SeatFindPort seatFindPort;

    /**
     * {@inheritDoc}
     *
     * <p>The seat is read through the not-deleted lookup, so a deleted seat is a missing seat here.
     */
    @Override
    public FindSeatResult find(FindSeatQuery query) {
        return seatFindPort
                .findNotDeletedById(query.seatId())
                .map(FindSeatResult::from)
                .orElseThrow(() -> new NoSuchElementException("seat does not exists."));
    }
}
