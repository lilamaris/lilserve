package com.lilamaris.lilserve.reservation.persistence.jpa.row;

/** Groups native query result records for reservation allocation persistence. */
public class ReservedScheduleRow {
    /**
     * Counts used to determine whether a request released every allocation of a reservation. The
     * update count describes this statement's changes, which remain provisional until commit.
     *
     * @param totalCount the total number of allocations, including previously released rows
     * @param updatedCount the number of active allocations released by this statement
     */
    public record ReleasedSchedule(long totalCount, long updatedCount) {}
}
