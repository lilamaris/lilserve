package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.reservation.application.port.in.ListRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Implements paginated room lookup in a read-only transaction. */
@Service
@Validated
@RequiredArgsConstructor
public class ListRoomService implements ListRoomUseCase {
    private final RoomFindPort roomFindPort;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public OffsetPage<FindRoomResult> list(ListRoomQuery query) {
        var page =
                roomFindPort.findNotDeletedByFilters(
                        query.name(), query.code(), query.pageRequest());
        var items = page.items().stream().map(FindRoomResult::from).toList();
        return OffsetPage.of(items, page.page(), page.size(), page.totalElements());
    }
}
