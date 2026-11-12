package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.constraints.NotNull;

/**
 * Requests a page of non-deleted seats in one non-deleted room.
 *
 * @param roomId the owning room
 * @param pageRequest the zero-based page and size
 */
public record ListSeatQuery(@NotNull RoomId roomId, @NotNull OffsetPageRequest pageRequest) {
    /**
     * Creates a seat page query, validated at the use case boundary.
     *
     * @param roomId the owning room
     * @param pageRequest the requested page
     * @return the query
     */
    public static ListSeatQuery of(RoomId roomId, OffsetPageRequest pageRequest) {
        return new ListSeatQuery(roomId, pageRequest);
    }
}
