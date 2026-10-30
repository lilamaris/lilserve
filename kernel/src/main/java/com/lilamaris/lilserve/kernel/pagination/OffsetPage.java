package com.lilamaris.lilserve.kernel.pagination;

import com.lilamaris.lilserve.kernel.condition.NumberPrecondition;
import java.util.List;

/**
 * A page of items with offset-based metadata. {@code page} is zero-based, and the total page count
 * of an empty result is 0.
 *
 * @param items the page items, unmodifiable
 * @param page the zero-based page number
 * @param size the maximum number of items per page
 * @param totalElements the total number of elements across all pages
 * @param <T> the item type
 */
public record OffsetPage<T>(List<T> items, int page, int size, long totalElements) {
    /**
     * @param items the page items; copied so the page is not backed by the caller's list
     * @param page the zero-based page number
     * @param size the maximum number of items per page
     * @param totalElements the total number of elements across all pages
     * @throws IllegalValueConditionException if {@code page} or {@code totalElements} is negative,
     *     or {@code size} is not positive
     * @throws IllegalArgumentException if {@code items} has more elements than {@code size}
     * @throws NullPointerException if {@code items} is null, or contains a null item
     */
    public OffsetPage {
        items = List.copyOf(items);
        NumberPrecondition.requireNonNegative(page);
        NumberPrecondition.requirePositive(size);
        NumberPrecondition.requireNonNegative(totalElements);
        if (items.size() > size) {
            throw new IllegalArgumentException("items count must not exceed size.");
        }
    }

    /**
     * Creates a page from already paginated items.
     *
     * @param items the page items
     * @param page the zero-based page number
     * @param size the maximum number of items per page
     * @param totalElements the total number of elements across all pages
     * @param <T> the item type
     * @return a page holding copies of {@code items} and the given metadata
     * @throws IllegalValueConditionException if {@code page} or {@code totalElements} is negative,
     *     or {@code size} is not positive
     * @throws IllegalArgumentException if {@code items} has more elements than {@code size}
     * @throws NullPointerException if {@code items} is null, or contains a null item
     */
    public static <T> OffsetPage<T> of(List<T> items, int page, int size, long totalElements) {
        return new OffsetPage<>(items, page, size, totalElements);
    }

    /**
     * Returns the total number of pages.
     *
     * @return the number of pages needed to cover {@code totalElements}
     */
    public long totalPages() {
        return totalElements / size + (totalElements % size == 0 ? 0 : 1);
    }

    /**
     * Reports whether a previous page exists.
     *
     * @return true when {@code page} is not the first page
     */
    public boolean hasPrevious() {
        return page > 0;
    }

    /**
     * Reports whether a next page exists.
     *
     * @return true when a page after {@code page} fits within {@code totalPages()}
     */
    public boolean hasNext() {
        return (long) page + 1 < totalPages();
    }
}
