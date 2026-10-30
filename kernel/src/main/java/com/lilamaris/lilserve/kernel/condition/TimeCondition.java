package com.lilamaris.lilserve.kernel.condition;

/** Conditions on the sign of a {@link java.time.Duration}. */
public enum TimeCondition implements Condition {
    NEGATIVE("be negative"),
    NON_NEGATIVE("be non-negative"),
    POSITIVE("be positive"),
    NON_POSITIVE("be non-positive");

    private final String phrase;

    TimeCondition(String phrase) {
        this.phrase = phrase;
    }

    /**
     * Returns the phrase of this time condition.
     *
     * @return the condition phrase
     */
    @Override
    public String phrase() {
        return phrase;
    }
}
