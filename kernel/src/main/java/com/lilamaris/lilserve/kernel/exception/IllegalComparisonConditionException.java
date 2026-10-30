package com.lilamaris.lilserve.kernel.exception;

import com.lilamaris.lilserve.kernel.condition.Condition;
import java.util.List;

/**
 * Condition violation judged from the relationship between a value and its references. Holds one or
 * more non-null references, in order.
 *
 * @param references the reference values, in order
 */
public final class IllegalComparisonConditionException extends IllegalConditionException {
    private final List<?> references;

    /**
     * @param subject the checked value
     * @param condition the violated condition
     * @param references the reference values, in order; at least one, none null
     * @throws IllegalArgumentException if {@code references} is empty
     * @throws NullPointerException if any reference is null
     */
    public IllegalComparisonConditionException(
            Object subject, Condition condition, Object... references) {
        this(subject, condition, requireReferences(references));
    }

    private IllegalComparisonConditionException(
            Object subject, Condition condition, List<?> references) {
        super(subject, condition, message(condition, references));
        this.references = references;
    }

    /**
     * Returns all reference values, in order.
     *
     * @return the unmodifiable list of references
     */
    public List<?> references() {
        return references;
    }

    /**
     * Returns the first reference value.
     *
     * @return the first reference
     */
    public Object reference() {
        return references.getFirst();
    }

    private static List<?> requireReferences(Object[] references) {
        if (references.length == 0) {
            throw new IllegalArgumentException("at least one reference is required.");
        }
        return List.of(references);
    }
}
