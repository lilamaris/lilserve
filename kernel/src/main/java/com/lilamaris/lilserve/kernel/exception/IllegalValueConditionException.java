package com.lilamaris.lilserve.kernel.exception;

import com.lilamaris.lilserve.kernel.condition.Condition;
import java.util.List;

/** Condition violation judged from the value's own state. It has no reference values. */
public final class IllegalValueConditionException extends IllegalConditionException {
    /**
     * Creates an exception describing a failed value condition.
     *
     * @param subject the checked value
     * @param condition the violated condition
     */
    public IllegalValueConditionException(Object subject, Condition condition) {
        super(subject, condition, message(condition, List.of()));
    }
}
