package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.reservation.application.port.in.ListSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Implements paginated seat lookup in a read-only transaction. */
@Service
@Validated
@RequiredArgsConstructor
public class ListSeatService implements ListSeatUseCase {
    private final SeatFindPort seatFindPort;
    private final RoomFindPort roomFindPort;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public OffsetPage<FindSeatResult> list(ListSeatQuery query) {
        roomFindPort
                .findNotDeletedById(query.roomId())
                .orElseThrow(() -> new NoSuchElementException("room does not exists."));
        var page = seatFindPort.findNotDeletedByRoomId(query.roomId(), query.pageRequest());
        var items = page.items().stream().map(FindSeatResult::from).toList();
        return OffsetPage.of(items, page.page(), page.size(), page.totalElements());
    }
}
