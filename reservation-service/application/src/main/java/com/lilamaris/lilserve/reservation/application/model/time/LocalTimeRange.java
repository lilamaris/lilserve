package com.lilamaris.lilserve.reservation.application.model.time;

import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import java.time.LocalTime;
import java.util.Objects;

/** A non-empty local time interval that ends on the same day it starts. */
public record LocalTimeRange(LocalTime startAt, LocalTime endAt) {
    /**
     * Validates the interval immediately, independent of service validation.
     *
     * @param startAt the inclusive start time, not null
     * @param endAt the exclusive end time, strictly after the start time
     * @throws NullPointerException if a time is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     end time is not after the start time
     */
    public LocalTimeRange {
        Objects.requireNonNull(startAt);
        TimePrecondition.requireAfter(endAt, startAt);
    }

    /**
     * Creates a validated time interval.
     *
     * @param startAt the inclusive start time
     * @param endAt the exclusive end time, strictly after the start time
     * @return the interval
     */
    public static LocalTimeRange of(LocalTime startAt, LocalTime endAt) {
        return new LocalTimeRange(startAt, endAt);
    }
}
