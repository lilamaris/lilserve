package com.lilamaris.lilserve.reservation.application.model.time;

import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import java.time.Instant;
import java.util.Objects;

/**
 * A non-empty, half-open interval on the absolute timeline.
 *
 * @param startsAt the inclusive start instant
 * @param endsAt the exclusive end instant, strictly after the start
 */
public record InstantRange(Instant startsAt, Instant endsAt) {
    /**
     * Validates both bounds immediately, independent of Bean Validation.
     *
     * @param startsAt the inclusive start instant, not null
     * @param endsAt the exclusive end instant, strictly after the start
     * @throws NullPointerException if either bound is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     end is not after the start
     */
    public InstantRange {
        Objects.requireNonNull(startsAt);
        TimePrecondition.requireAfter(endsAt, startsAt);
    }

    /**
     * Creates a validated absolute time interval.
     *
     * @param startsAt the inclusive start instant
     * @param endsAt the exclusive end instant, strictly after the start
     * @return the interval
     * @throws NullPointerException if either bound is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     end is not after the start
     */
    public static InstantRange of(Instant startsAt, Instant endsAt) {
        return new InstantRange(startsAt, endsAt);
    }
}
