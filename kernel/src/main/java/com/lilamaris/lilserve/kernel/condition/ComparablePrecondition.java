package com.lilamaris.lilserve.kernel.condition;

import com.lilamaris.lilserve.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import java.util.Objects;

/** Shared implementation of range checks on the natural order. Ranges include both endpoints. */
final class ComparablePrecondition {
    private ComparablePrecondition() {}

    /**
     * Requires that {@code value} is at least {@code minimum}.
     *
     * @param value the value to check
     * @param minimum the inclusive lower bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is below {@code minimum}
     * @throws NullPointerException if {@code value} or {@code minimum} is null
     */
    static <T extends Comparable<? super T>> T requireAtLeast(T value, T minimum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(minimum, "minimum must not be null.");
        if (value.compareTo(minimum) < 0) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.AT_LEAST, minimum);
        }
        return value;
    }

    /**
     * Requires that {@code value} is at most {@code maximum}.
     *
     * @param value the value to check
     * @param maximum the inclusive upper bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is above {@code maximum}
     * @throws NullPointerException if {@code value} or {@code maximum} is null
     */
    static <T extends Comparable<? super T>> T requireAtMost(T value, T maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(maximum, "maximum must not be null.");
        if (value.compareTo(maximum) > 0) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.AT_MOST, maximum);
        }
        return value;
    }

    /**
     * Requires that {@code value} is between {@code minimum} and {@code maximum}, both ends
     * included.
     *
     * @param value the value to check
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is outside the range
     * @throws IllegalBoundsException if {@code minimum} is above {@code maximum}
     * @throws NullPointerException if any argument is null
     */
    static <T extends Comparable<? super T>> T requireBetween(T value, T minimum, T maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        Objects.requireNonNull(minimum, "minimum must not be null.");
        Objects.requireNonNull(maximum, "maximum must not be null.");
        if (minimum.compareTo(maximum) > 0) {
            throw new IllegalBoundsException(minimum, maximum);
        }
        if (isOutsideRange(value, minimum, maximum)) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.BETWEEN, minimum, maximum);
        }
        return value;
    }

    private static <T extends Comparable<? super T>> boolean isOutsideRange(
            T value, T minimum, T maximum) {
        return value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0;
    }
}
