package com.lilamaris.lilserve.kernel.exception;

import com.lilamaris.lilserve.kernel.condition.Condition;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common ancestor of failed condition checks. Carries the checked {@code subject} and the violated
 * {@code condition}. The message describes the condition only, and never the name of the subject.
 *
 * @param subject the checked value
 * @param condition the violated condition
 */
public abstract sealed class IllegalConditionException extends IllegalArgumentException
        permits IllegalValueConditionException, IllegalComparisonConditionException {
    private final Object subject;
    private final Condition condition;

    /**
     * @param subject the checked value
     * @param condition the violated condition
     * @param message the failure message
     */
    protected IllegalConditionException(Object subject, Condition condition, String message) {
        super(message);
        this.subject = subject;
        this.condition = condition;
    }

    /**
     * Assembles the failure message from a condition and its references.
     *
     * @param condition the violated condition
     * @param references the reference values, in order
     * @return "must " + condition phrase + references + "."
     */
    protected static String message(Condition condition, List<?> references) {
        return "must " + condition.phrase() + describeReferences(references) + ".";
    }

    private static String describeReferences(List<?> references) {
        if (references.isEmpty()) {
            return "";
        }
        String separator = references.size() > 1 ? " and " : " ";
        return " " + joinReferences(references, separator);
    }

    private static String joinReferences(List<?> references, String separator) {
        return references.stream().map(String::valueOf).collect(Collectors.joining(separator));
    }

    /**
     * Returns the checked value.
     *
     * @return the checked value
     */
    public Object subject() {
        return subject;
    }

    /**
     * Returns the violated condition.
     *
     * @return the violated condition
     */
    public Condition condition() {
        return condition;
    }
}
