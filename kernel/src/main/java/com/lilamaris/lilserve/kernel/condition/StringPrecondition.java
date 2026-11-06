package com.lilamaris.lilserve.kernel.condition;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.Objects;

/** Checks string content and length constraints. Null and blank handling depends on the check. */
public final class StringPrecondition {
    private StringPrecondition() {}

    /**
     * Requires that {@code value} is not blank.
     *
     * @param value the value to check
     * @return {@code value} unchanged
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException if {@code
     *     value} is blank
     * @throws NullPointerException if {@code value} is null
     */
    public static String requireNonBlank(String value) {
        Objects.requireNonNull(value, "value must not be null.");
        if (value.isBlank()) {
            throw new IllegalValueConditionException(value, StringCondition.NOT_BLANK);
        }
        return value;
    }

    /**
     * Requires that {@code value} is non-blank and does not contain {@code text}.
     *
     * @param value the value to check
     * @param text the text that must not occur in {@code value}
     * @return {@code value} unchanged
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException if {@code
     *     value} is blank
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     value} contains {@code text}
     * @throws NullPointerException if {@code value} or {@code text} is null
     */
    public static String requireNotContain(String value, String text) {
        requireNonBlank(value);
        Objects.requireNonNull(text, "text must not be null.");
        if (value.contains(text)) {
            throw new IllegalComparisonConditionException(
                    value, ComparisonCondition.NOT_CONTAIN, text);
        }
        return value;
    }

    /**
     * Requires that {@code value} is non-null and its length is at most {@code maxLen}. Blank
     * values are accepted.
     *
     * @param value the value to check, not null
     * @param maxLen the inclusive maximum length
     * @return {@code value} unchanged
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     length is above {@code maxLen}
     * @throws NullPointerException if {@code value} is null
     */
    public static String requireNonNullMaxLen(String value, int maxLen) {
        Objects.requireNonNull(value, "value must not be null.");
        NumberPrecondition.requireAtMost(value.length(), maxLen);
        return value;
    }

    /**
     * Requires that {@code value} is non-blank and its length is at most {@code maxLen}.
     *
     * @param value the value to check
     * @param maxLen the inclusive maximum length
     * @return {@code value} unchanged
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException if {@code
     *     value} is blank
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     length is above {@code maxLen}
     * @throws NullPointerException if {@code value} is null
     */
    public static String requireMaxLen(String value, int maxLen) {
        requireNonBlank(value);
        NumberPrecondition.requireAtMost(value.length(), maxLen);
        return value;
    }
}
