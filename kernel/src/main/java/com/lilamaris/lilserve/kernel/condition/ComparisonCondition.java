package com.lilamaris.lilserve.kernel.condition;

/** Conditions on the relationship between a value and one or more reference values. */
public enum ComparisonCondition implements Condition {
    BEFORE("be before"),
    BEFORE_OR_EQUAL("be before or equal to"),
    AFTER("be after"),
    AFTER_OR_EQUAL("be after or equal to"),
    AT_LEAST("be at least"),
    AT_MOST("be at most"),
    BETWEEN("be between"),
    NOT_CONTAIN("not contain");

    private final String phrase;

    ComparisonCondition(String phrase) {
        this.phrase = phrase;
    }

    /**
     * Returns the phrase of this comparison condition.
     *
     * @return the condition phrase
     */
    @Override
    public String phrase() {
        return phrase;
    }
}
