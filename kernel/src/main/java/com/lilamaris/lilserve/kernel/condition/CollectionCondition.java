package com.lilamaris.lilserve.kernel.condition;

/**
 * Conditions on a collection: element nullness and size bounds. A size bound is non-negative, and a
 * range includes both endpoints.
 */
public enum CollectionCondition implements Condition {
    NULL_ELEMENT("not contain null elements"),
    SIZE_AT_LEAST("have at least"),
    SIZE_AT_MOST("have at most"),
    SIZE_BETWEEN("have between");

    private final String phrase;

    CollectionCondition(String phrase) {
        this.phrase = phrase;
    }

    /**
     * Returns the phrase of this collection condition.
     *
     * @return the condition phrase
     */
    @Override
    public String phrase() {
        return phrase;
    }
}
