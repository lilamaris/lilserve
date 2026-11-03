package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The driving port that finds one room. */
public interface FindRoomUseCase {

    /**
     * Finds the non-deleted room the query targets.
     *
     * @param query the find request, must not be null
     * @return the found room
     * @throws java.util.NoSuchElementException if the room does not exist or is deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the query is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code query} is null
     */
    FindRoomResult find(@NotNull @Valid FindRoomQuery query);
}
