package com.lilamaris.lilserve.kernel.condition;

/** Conditions on a string value. */
public enum StringCondition implements Condition {
    NOT_BLANK("not be blank");

    private final String phrase;

    StringCondition(String phrase) {
        this.phrase = phrase;
    }

    /**
     * Returns the phrase of this string condition.
     *
     * @return the condition phrase
     */
    @Override
    public String phrase() {
        return phrase;
    }
}
