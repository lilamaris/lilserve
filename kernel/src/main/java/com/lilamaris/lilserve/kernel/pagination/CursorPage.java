package com.lilamaris.lilserve.kernel.pagination;

import com.lilamaris.lilserve.kernel.condition.NumberPrecondition;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A page of items with the cursor of the last item. A null {@code nextCursor} means there is no
 * next page.
 *
 * @param items the page items, unmodifiable
 * @param nextCursor the cursor of the last item, or null when there is no next page
 * @param <T> the item type
 * @param <I> the cursor id type
 */
public record CursorPage<T, I>(List<T> items, Cursor<I> nextCursor) {
    /** Copies the items so the page is not backed by the caller's list. */
    public CursorPage {
        items = List.copyOf(items);
    }

    /**
     * Creates a page from already paginated items.
     *
     * @param items the page items
     * @param nextCursor the cursor of the last item, or null when there is no next page
     * @param <T> the item type
     * @param <I> the cursor id type
     * @return a page holding copies of {@code items} and {@code nextCursor}
     * @throws NullPointerException if {@code items} is null, or contains a null item
     */
    public static <T, I> CursorPage<T, I> of(List<T> items, Cursor<I> nextCursor) {
        return new CursorPage<>(items, nextCursor);
    }

    /**
     * Converts the result of fetching up to {@code limit + 1} items in the same sort order into a
     * page.
     *
     * @param fetched the fetched items, in sort order
     * @param limit the maximum number of items to return, leaving room for one extra fetch
     * @param cursorExtractor extracts the cursor of an item
     * @param <T> the item type
     * @param <I> the cursor id type
     * @return a page with at most {@code limit} items and the cursor of the last returned item
     * @throws IllegalValueConditionExceptior if {@code limit} is not positive
     * @throws IllegalComparisonConditionException if {@code limit} leaves no room for the extra
     *     fetch
     * @throws NullPointerException if {@code fetched} or {@code cursorExtractor} is null, if a
     *     fetched item is null, or if the extracted cursor is null
     */
    public static <T, I> CursorPage<T, I> from(
            List<T> fetched, int limit, Function<? super T, Cursor<I>> cursorExtractor) {
        NumberPrecondition.requirePositive(limit);
        NumberPrecondition.requireAtMost(limit, Integer.MAX_VALUE - 1);
        Objects.requireNonNull(cursorExtractor, "cursorExtractor must not be null.");
        List<T> snapshot = List.copyOf(fetched);
        List<T> pageItems = itemsUpTo(snapshot, limit);
        return new CursorPage<>(pageItems, nextCursor(snapshot, pageItems, cursorExtractor));
    }

    /**
     * Reports whether a next page exists.
     *
     * @return true when {@code nextCursor} is not null
     */
    public boolean hasNext() {
        return nextCursor != null;
    }

    private static <T> List<T> itemsUpTo(List<T> fetched, int limit) {
        return fetched.subList(0, Math.min(fetched.size(), limit));
    }

    private static <T, I> Cursor<I> nextCursor(
            List<T> fetched, List<T> pageItems, Function<? super T, Cursor<I>> cursorExtractor) {
        if (fetched.size() <= pageItems.size()) {
            return null;
        }
        return Objects.requireNonNull(
                cursorExtractor.apply(pageItems.getLast()), "nextCursor must not be null.");
    }
}
