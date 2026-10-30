package com.lilamaris.lilserve.kernel.condition;

/** Conditions on the sign of a number. */
public enum NumberCondition implements Condition {
    NEGATIVE("be negative"),
    NON_NEGATIVE("be non-negative"),
    POSITIVE("be positive"),
    NON_POSITIVE("be non-positive");

    private final String phrase;

    NumberCondition(String phrase) {
        this.phrase = phrase;
    }

    /**
     * Returns the phrase of this number condition.
     *
     * @return the condition phrase
     */
    @Override
    public String phrase() {
        return phrase;
    }
}
