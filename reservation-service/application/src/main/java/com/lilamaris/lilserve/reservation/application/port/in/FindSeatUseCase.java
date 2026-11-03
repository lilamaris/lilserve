package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The driving port that finds one seat. */
public interface FindSeatUseCase {

    /**
     * Finds the non-deleted seat the query targets.
     *
     * @param query the find request, must not be null
     * @return the found seat
     * @throws java.util.NoSuchElementException if the seat does not exist or is deleted
     * @throws jakarta.validation.ConstraintViolationException if method validation is active and
     *     the query is null or violates its constraints
     * @throws NullPointerException if method validation is bypassed and {@code query} is null
     */
    FindSeatResult find(@NotNull @Valid FindSeatQuery query);
}
