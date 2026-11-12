package com.lilamaris.lilserve.reservation.application.port.in.query;

import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Requests a page of active rooms. Filters are case-insensitive literal substrings and are combined
 * with AND. Null or blank filters are ignored.
 *
 * @param name optional room name fragment
 * @param code optional room code fragment
 * @param pageRequest the zero-based page and size
 */
public record ListRoomQuery(
        @Nullable String name, @Nullable String code, @NotNull OffsetPageRequest pageRequest) {
    /**
     * Normalizes optional filters by stripping surrounding whitespace and ignoring blank values.
     *
     * @param name optional room name fragment
     * @param code optional room code fragment
     * @param pageRequest the requested page
     */
    public ListRoomQuery {
        name = normalize(name);
        code = normalize(code);
    }

    /**
     * Creates a filtered room page query.
     *
     * @param name optional room name fragment
     * @param code optional room code fragment
     * @param pageRequest the requested page
     * @return the normalized query
     */
    public static ListRoomQuery of(
            @Nullable String name, @Nullable String code, OffsetPageRequest pageRequest) {
        return new ListRoomQuery(name, code, pageRequest);
    }

    private static String normalize(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
