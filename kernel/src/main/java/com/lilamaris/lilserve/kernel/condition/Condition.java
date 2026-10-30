package com.lilamaris.lilserve.kernel.condition;

/**
 * Vocabulary of a condition check. A failure message is assembled as {@code "must "} + {@link
 * #phrase()} + references.
 */
public interface Condition {
    /**
     * Returns the phrase that completes "must ".
     *
     * @return the condition phrase
     */
    String phrase();
}
