package com.lilamaris.lilserve.kernel.pagination;

import com.lilamaris.lilserve.kernel.condition.NumberPrecondition;

/**
 * A cursor page request. A null {@code cursor} requests the first page. {@code size} is the maximum
 * number of items to fetch, and must stay below {@code Integer.MAX_VALUE} so that one extra item
 * can be fetched to detect the next page.
 *
 * @param cursor the position to continue from, or null for the first page
 * @param size the maximum number of items to fetch
 * @param <I> the cursor id type
 */
public record CursorPageRequest<I>(Cursor<I> cursor, int size) {
    /**
     * @param cursor the position to continue from, or null for the first page
     * @param size the maximum number of items to fetch; must leave room for one extra fetch
     * @throws IllegalValueConditionException if {@code size} is not positive
     * @throws IllegalComparisonConditionException if {@code size} leaves no room for the extra
     *     fetch
     */
    public CursorPageRequest {
        NumberPrecondition.requirePositive(size);
        NumberPrecondition.requireAtMost(size, Integer.MAX_VALUE - 1);
    }

    /**
     * Creates a page request.
     *
     * @param cursor the position to continue from, or null for the first page
     * @param size the maximum number of items to fetch
     * @param <I> the cursor id type
     * @return a request with the given cursor and size
     * @throws IllegalValueConditionException if {@code size} is not positive
     * @throws IllegalComparisonConditionException if {@code size} leaves no room for the extra
     *     fetch
     */
    public static <I> CursorPageRequest<I> of(Cursor<I> cursor, int size) {
        return new CursorPageRequest<>(cursor, size);
    }

    /**
     * Reports whether this request is for the first page.
     *
     * @return true when {@code cursor} is null
     */
    public boolean isFirstPage() {
        return cursor == null;
    }
}
