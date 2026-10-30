package com.lilamaris.lilserve.kernel.condition;

import com.lilamaris.lilserve.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.Collection;
import java.util.Objects;

/**
 * Checks conditions on a collection. Size bounds are non-negative, and a range includes both
 * endpoints.
 */
public final class CollectionPrecondition {
    private CollectionPrecondition() {}

    /**
     * Requires that {@code value} contains no null elements.
     *
     * @param value the collection to check
     * @param <E> the element type
     * @param <C> the collection type
     * @return {@code value} unchanged
     * @throws IllegalValueConditionException if {@code value} contains a null element
     * @throws NullPointerException if {@code value} is null
     */
    public static <E, C extends Collection<E>> C requireNonNullElements(C value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (containsNullElement(value)) {
            throw new IllegalValueConditionException(value, CollectionCondition.NULL_ELEMENT);
        }
        return value;
    }

    /**
     * Requires that {@code value} has at least {@code minimum} elements.
     *
     * @param value the collection to check
     * @param minimum the inclusive lower bound of the size
     * @param <E> the element type
     * @param <C> the collection type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if the size is below {@code minimum}
     * @throws IllegalArgumentException if {@code minimum} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static <E, C extends Collection<E>> C requireSizeAtLeast(C value, int minimum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(minimum, "minimum");
        if (value.size() < minimum) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_AT_LEAST, minimum);
        }
        return value;
    }

    /**
     * Requires that {@code value} has at most {@code maximum} elements.
     *
     * @param value the collection to check
     * @param maximum the inclusive upper bound of the size
     * @param <E> the element type
     * @param <C> the collection type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if the size is above {@code maximum}
     * @throws IllegalArgumentException if {@code maximum} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static <E, C extends Collection<E>> C requireSizeAtMost(C value, int maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(maximum, "maximum");
        if (value.size() > maximum) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_AT_MOST, maximum);
        }
        return value;
    }

    /**
     * Requires that {@code value} has between {@code minimum} and {@code maximum} elements, both
     * ends included.
     *
     * @param value the collection to check
     * @param minimum the inclusive lower bound of the size
     * @param maximum the inclusive upper bound of the size
     * @param <E> the element type
     * @param <C> the collection type
     * @return {@code value} unchanged
     * @throws IllegalComparisonConditionException if the size is outside the range
     * @throws IllegalBoundsException if {@code minimum} is above {@code maximum}
     * @throws IllegalArgumentException if {@code minimum} or {@code maximum} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static <E, C extends Collection<E>> C requireSizeBetween(
            C value, int minimum, int maximum) {
        Objects.requireNonNull(value, "value must not be null.");
        requireNonNegativeSize(minimum, "minimum");
        requireNonNegativeSize(maximum, "maximum");
        if (minimum > maximum) {
            throw new IllegalBoundsException(minimum, maximum);
        }
        if (isOutsideRange(value.size(), minimum, maximum)) {
            throw new IllegalComparisonConditionException(
                    value, CollectionCondition.SIZE_BETWEEN, minimum, maximum);
        }
        return value;
    }

    private static <E> boolean containsNullElement(Collection<E> value) {
        return value.stream().anyMatch(Objects::isNull);
    }

    private static boolean isOutsideRange(int size, int minimum, int maximum) {
        return size < minimum || size > maximum;
    }

    private static void requireNonNegativeSize(int size, String name) {
        if (size < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}
