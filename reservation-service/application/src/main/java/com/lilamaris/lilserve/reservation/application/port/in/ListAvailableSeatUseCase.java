package com.lilamaris.lilserve.reservation.application.port.in;

import com.lilamaris.lilserve.reservation.application.port.in.query.ListAvailableSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.ListAvailableSeatResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Looks up seats available for a selection of recurring slots on one service date. */
public interface ListAvailableSeatUseCase {
    /**
     * Lists available seat identifiers in a non-deleted room.
     *
     * <p>Returns an empty seat list when no slots are selected or any selected service interval
     * overlaps a non-deleted room usage restriction. Availability is advisory; reservation creation
     * rechecks it.
     *
     * @param query the room, service date, and selected slots, validated at the use case boundary
     * @return the room, service date, and available seat identifiers in code and identifier order
     * @throws java.util.NoSuchElementException if the room does not exist or is deleted
     */
    ListAvailableSeatResult list(@NotNull @Valid ListAvailableSeatQuery query);
}
