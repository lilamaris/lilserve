package com.lilamaris.lilserve.reservation.application.port.out;

/**
 * The outcome of releasing a reservation's allocations within the caller's transaction. Changes
 * remain provisional until commit; callers requiring a complete cancellation must roll back any
 * outcome other than {@link #ALL_RELEASED}.
 */
public enum ReleaseScheduleStatus {
    /** The reservation has allocations and this request released all of them. */
    ALL_RELEASED,
    /** This request released some allocations, while others had already been released. */
    PARTIAL_RELEASED,
    /**
     * This request released no allocations, including when none exist or all are already released.
     */
    NONE_RELEASED
}
