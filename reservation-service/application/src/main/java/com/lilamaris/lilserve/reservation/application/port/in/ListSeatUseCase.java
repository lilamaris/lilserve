package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Lists non-deleted seats in pages ordered by code. */
public interface ListSeatUseCase {
    /**
     * Returns a database-paginated snapshot with the total number of matching seats.
     *
     * @param query the filters and requested page
     * @return the requested page, empty when no items match or the page is beyond the result
     * @throws java.util.NoSuchElementException if the room is missing or deleted
     */
    OffsetPage<FindSeatResult> list(@NotNull @Valid ListSeatQuery query);

    /**
     * Lists deleted seats in a non-deleted room.
     *
     * @param query the required room identifier and page request
     * @return the page ordered by code and identifier, with the room's deleted seat total; empty
     *     when no seats match or the page is beyond the result
     * @throws java.util.NoSuchElementException if the room is missing or deleted
     */
    OffsetPage<FindSeatResult> listDeleted(@NotNull @Valid ListSeatQuery query);
}
