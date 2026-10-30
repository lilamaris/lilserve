package com.lilamaris.lilserve.kernel.exception;

/**
 * Invalid definition of the bounds themselves, not a violation of a value. It is separated from the
 * condition-violation family because the meanings differ.
 *
 * @param minimum the reported lower bound
 * @param maximum the reported upper bound
 */
public final class IllegalBoundsException extends IllegalArgumentException {
    private final Object minimum;
    private final Object maximum;

    /**
     * @param minimum the lower bound
     * @param maximum the upper bound
     */
    public IllegalBoundsException(Object minimum, Object maximum) {
        super("minimum must be less than or equal to maximum.");
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Returns the lower bound that was reported.
     *
     * @return the lower bound
     */
    public Object minimum() {
        return minimum;
    }

    /**
     * Returns the upper bound that was reported.
     *
     * @return the upper bound
     */
    public Object maximum() {
        return maximum;
    }
}
