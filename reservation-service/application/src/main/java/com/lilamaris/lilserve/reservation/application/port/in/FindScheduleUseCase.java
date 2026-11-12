package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.query.FindScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindScheduleResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindScheduleUseCase {
    /**
     * Returns the non-deleted recurring slots of a non-deleted room, including all blackouts active
     * at the requested instant. Result ordering is unspecified.
     *
     * @param query the room and reservation intake instant
     * @return the room slots and their active blackouts, possibly empty
     * @throws java.util.NoSuchElementException if the room is missing or deleted
     */
    FindScheduleResult find(@NotNull @Valid FindScheduleQuery query);
}
