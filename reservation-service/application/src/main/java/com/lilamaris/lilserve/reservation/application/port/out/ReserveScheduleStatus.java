package com.lilamaris.lilserve.reservation.application.port.out;

/**
 * The outcome of allocating the requested slots within the caller's transaction. Changes remain
 * provisional until commit; callers requiring a complete reservation must roll back any outcome
 * other than {@link #ALL_RESERVED}.
 */
public enum ReserveScheduleStatus {
    /** This request allocated every requested slot to the reservation. */
    ALL_RESERVED,
    /** This request allocated some, but not all, of the requested slots. */
    PARTIAL_RESERVED,
    /** This request allocated none of the requested slots. */
    NONE_RESERVED
}
