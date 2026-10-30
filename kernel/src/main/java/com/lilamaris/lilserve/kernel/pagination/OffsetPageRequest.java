package com.lilamaris.lilserve.kernel.pagination;

import com.lilamaris.lilserve.kernel.condition.NumberPrecondition;

/**
 * An offset page request. {@code page} is zero-based, and {@code size} is the maximum number of
 * items to fetch. The offset must not exceed {@code Integer.MAX_VALUE}.
 *
 * @param page the zero-based page number
 * @param size the maximum number of items per page
 */
public record OffsetPageRequest(int page, int size) {
    /**
     * @param page the zero-based page number
     * @param size the maximum number of items per page
     * @throws IllegalValueConditionException if {@code page} is negative, or {@code size} is not
     *     positive
     * @throws IllegalComparisonConditionException if the offset exceeds {@code Integer.MAX_VALUE}
     */
    public OffsetPageRequest {
        NumberPrecondition.requireNonNegative(page);
        NumberPrecondition.requirePositive(size);
        NumberPrecondition.requireAtMost((long) page * size, (long) Integer.MAX_VALUE);
    }

    /**
     * Creates a page request.
     *
     * @param page the zero-based page number
     * @param size the maximum number of items per page
     * @return a request with the given page and size
     * @throws IllegalValueConditionException if {@code page} is negative, or {@code size} is not
     *     positive
     * @throws IllegalComparisonConditionException if the offset exceeds {@code Integer.MAX_VALUE}
     */
    public static OffsetPageRequest of(int page, int size) {
        return new OffsetPageRequest(page, size);
    }

    /**
     * Returns the offset of this page, computed without overflow.
     *
     * @return {@code page * size}
     */
    public long offset() {
        return (long) page * size;
    }
}
