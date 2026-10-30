package com.lilamaris.lilserve.kernel.pagination;

import java.time.Instant;
import java.util.Objects;

/**
 * The position of the last fetched item in a list ordered by creation time and unique id.
 *
 * @param createdAt creation time of the last fetched item
 * @param id unique id of the last fetched item
 * @param <I> the id type
 */
public record Cursor<I>(Instant createdAt, I id) {
    /**
     * @param createdAt creation time of the last fetched item; must not be null
     * @param id unique id of the last fetched item; must not be null
     * @throws NullPointerException if {@code createdAt} or {@code id} is null
     */
    public Cursor {
        Objects.requireNonNull(createdAt, "createdAt must not be null.");
        Objects.requireNonNull(id, "id must not be null.");
    }

    /**
     * Creates a cursor pointing at the item created at {@code createdAt} with {@code id}.
     *
     * @param createdAt creation time of the last fetched item
     * @param id unique id of the last fetched item
     * @param <I> the id type
     * @return a cursor for that position
     * @throws NullPointerException if {@code createdAt} or {@code id} is null
     */
    public static <I> Cursor<I> of(Instant createdAt, I id) {
        return new Cursor<>(createdAt, id);
    }
}
