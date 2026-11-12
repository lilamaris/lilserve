package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.query.ListScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.ListScheduleResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Lists the recurring slots of one room with the blackouts active at the requested instant. */
public interface ListScheduleUseCase {
    /**
     * Returns the non-deleted recurring slots of a non-deleted room, including all blackouts active
     * at the requested instant. Result ordering is unspecified.
     *
     * @param query the room and reservation intake instant
     * @return the room slots and their active blackouts, possibly empty
     * @throws java.util.NoSuchElementException if the room is missing or deleted
     */
    ListScheduleResult list(@NotNull @Valid ListScheduleQuery query);
}
