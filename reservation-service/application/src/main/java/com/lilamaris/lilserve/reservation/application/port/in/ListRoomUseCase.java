package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Lists non-deleted rooms in pages ordered by code. */
public interface ListRoomUseCase {
    /**
     * Returns a database-paginated snapshot with the total number of matching rooms.
     *
     * @param query the filters and requested page
     * @return the requested page, empty when no items match or the page is beyond the result
     */
    OffsetPage<FindRoomResult> list(@NotNull @Valid ListRoomQuery query);
}
