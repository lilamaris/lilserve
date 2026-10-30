package com.lilamaris.lilserve.kernel.condition;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Duration;
import java.util.Objects;

/** Checks the natural order of time values and the sign of a {@link Duration}. */
public final class TimePrecondition {
    private TimePrecondition() {}

    /**
     * Requires that {@code subject} is strictly before {@code upperBound}.
     *
     * @param subject the time value to check
     * @param upperBound the exclusive upper bound
     * @param <T> the comparable type
     * @return {@code subject} unchanged
     * @throws IllegalComparisonConditionException if {@code subject} is not before {@code
     *     upperBound}
     * @throws NullPointerException if {@code subject} or {@code upperBound} is null
     */
    public static <T extends Comparable<? super T>> T requireBefore(T subject, T upperBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(upperBound, "upperBound must not be null.");
        if (subject.compareTo(upperBound) >= 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.BEFORE, upperBound);
        }
        return subject;
    }

    /**
     * Requires that {@code subject} is before or equal to {@code upperBound}.
     *
     * @param subject the time value to check
     * @param upperBound the inclusive upper bound
     * @param <T> the comparable type
     * @return {@code subject} unchanged
     * @throws IllegalComparisonConditionException if {@code subject} is after {@code upperBound}
     * @throws NullPointerException if {@code subject} or {@code upperBound} is null
     */
    public static <T extends Comparable<? super T>> T requireBeforeOrEqual(
            T subject, T upperBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(upperBound, "upperBound must not be null.");
        if (subject.compareTo(upperBound) > 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.BEFORE_OR_EQUAL, upperBound);
        }
        return subject;
    }

    /**
     * Requires that {@code subject} is strictly after {@code lowerBound}.
     *
     * @param subject the time value to check
     * @param lowerBound the exclusive lower bound
     * @param <T> the comparable type
     * @return {@code subject} unchanged
     * @throws IllegalComparisonConditionException if {@code subject} is not after {@code
     *     lowerBound}
     * @throws NullPointerException if {@code subject} or {@code lowerBound} is null
     */
    public static <T extends Comparable<? super T>> T requireAfter(T subject, T lowerBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(lowerBound, "lowerBound must not be null.");
        if (subject.compareTo(lowerBound) <= 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.AFTER, lowerBound);
        }
        return subject;
    }

    /**
     * Requires that {@code subject} is after or equal to {@code lowerBound}.
     *
     * @param subject the time value to check
     * @param lowerBound the inclusive lower bound
     * @param <T> the comparable type
     * @return {@code subject} unchanged
     * @throws IllegalComparisonConditionException if {@code subject} is before {@code lowerBound}
     * @throws NullPointerException if {@code subject} or {@code lowerBound} is null
     */
    public static <T extends Comparable<? super T>> T requireAfterOrEqual(T subject, T lowerBound) {
        Objects.requireNonNull(subject, "subject must not be null.");
        Objects.requireNonNull(lowerBound, "lowerBound must not be null.");
        if (subject.compareTo(lowerBound) < 0) {
            throw new IllegalComparisonConditionException(
                    subject, ComparisonCondition.AFTER_OR_EQUAL, lowerBound);
        }
        return subject;
    }

    /**
     * Requires that {@code value} is at least {@code minimum}.
     *
     * @param value the time value to check
     * @param minimum the inclusive lower bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is below {@code minimum}
     * @throws NullPointerException if {@code value} or {@code minimum} is null
     */
    public static <T extends Comparable<? super T>> T requireAtLeast(T value, T minimum) {
        return ComparablePrecondition.requireAtLeast(value, minimum);
    }

    /**
     * Requires that {@code value} is at most {@code maximum}.
     *
     * @param value the time value to check
     * @param maximum the inclusive upper bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is above {@code maximum}
     * @throws NullPointerException if {@code value} or {@code maximum} is null
     */
    public static <T extends Comparable<? super T>> T requireAtMost(T value, T maximum) {
        return ComparablePrecondition.requireAtMost(value, maximum);
    }

    /**
     * Requires that {@code value} is between {@code minimum} and {@code maximum}, both ends
     * included.
     *
     * @param value the time value to check
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param <T> the comparable type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if {@code value} is outside the range
     * @throws IllegalBoundsException if {@code minimum} is above {@code maximum}
     * @throws NullPointerException if any argument is null
     */
    public static <T extends Comparable<? super T>> T requireBetween(
            T value, T minimum, T maximum) {
        return ComparablePrecondition.requireBetween(value, minimum, maximum);
    }

    /**
     * Requires that {@code value} is a negative duration.
     *
     * @param value the duration to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Duration requireNegative(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (!value.isNegative()) {
            throw new IllegalValueConditionException(value, TimeCondition.NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is a zero or positive duration.
     *
     * @param value the duration to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Duration requireNonNegative(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isNegative()) {
            throw new IllegalValueConditionException(value, TimeCondition.NON_NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is a positive duration.
     *
     * @param value the duration to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Duration requirePositive(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (!value.isPositive()) {
            throw new IllegalValueConditionException(value, TimeCondition.POSITIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is a zero or negative duration.
     *
     * @param value the duration to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Duration requireNonPositive(Duration value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isPositive()) {
            throw new IllegalValueConditionException(value, TimeCondition.NON_POSITIVE);
        }
        return value;
    }
}
