package com.lilamaris.lilserve.kernel.condition;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.Objects;

/** Checks sign conditions on numbers, and range conditions on comparable values. */
public final class NumberPrecondition {
    private NumberPrecondition() {}

    /**
     * Requires that {@code value} is negative.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Integer requireNegative(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value >= 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is zero or positive.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Integer requireNonNegative(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value < 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NON_NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is positive.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Integer requirePositive(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value <= 0) {
            throw new IllegalValueConditionException(value, NumberCondition.POSITIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is zero or negative.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Integer requireNonPositive(Integer value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value > 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NON_POSITIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is negative.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Long requireNegative(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value >= 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is zero or positive.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Long requireNonNegative(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value < 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NON_NEGATIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is positive.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is zero or negative
     * @throws NullPointerException if {@code value} is null
     */
    public static Long requirePositive(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value <= 0) {
            throw new IllegalValueConditionException(value, NumberCondition.POSITIVE);
        }
        return value;
    }

    /**
     * Requires that {@code value} is zero or negative.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} is positive
     * @throws NullPointerException if {@code value} is null
     */
    public static Long requireNonPositive(Long value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value > 0) {
            throw new IllegalValueConditionException(value, NumberCondition.NON_POSITIVE);
        }
        return value;
    }

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
    public static <T extends Comparable<? super T>> T requireAtLeast(T value, T minimum) {
        return ComparablePrecondition.requireAtLeast(value, minimum);
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
    public static <T extends Comparable<? super T>> T requireAtMost(T value, T maximum) {
        return ComparablePrecondition.requireAtMost(value, maximum);
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
    public static <T extends Comparable<? super T>> T requireBetween(
            T value, T minimum, T maximum) {
        return ComparablePrecondition.requireBetween(value, minimum, maximum);
    }
}
