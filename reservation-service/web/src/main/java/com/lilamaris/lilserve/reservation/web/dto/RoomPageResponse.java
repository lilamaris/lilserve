package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One page of rooms with the offset metadata the web layer returns.
 *
 * <p>Every field is always present, so the document publishes all four as required. The items are
 * the rooms of this page in code order, and an empty page is answered with an empty array.
 *
 * @param items the rooms of this page
 * @param page the zero-based page number
 * @param size the maximum number of items per page
 * @param totalElements the number of matching rooms across all pages
 */
@Schema(
        description =
                "One page of rooms with offset metadata. An empty page is answered with an empty"
                        + " items array.",
        example = OpenApiExamples.ROOM_PAGE)
public record RoomPageResponse(
        @Schema(
                        description = "rooms of this page, in code order",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<RoomResponse> items,
        @Schema(
                        description = "zero-based page number",
                        type = "integer",
                        format = "int32",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                int page,
        @Schema(
                        description = "maximum number of items per page",
                        type = "integer",
                        format = "int32",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                int size,
        @Schema(
                        description = "number of matching rooms across all pages",
                        type = "integer",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long totalElements) {

    /**
     * Maps a page of room responses.
     *
     * @param page the page of mapped rooms with its metadata
     * @return the response body
     * @throws NullPointerException if {@code page} is null
     */
    public static RoomPageResponse of(OffsetPage<RoomResponse> page) {
        return new RoomPageResponse(page.items(), page.page(), page.size(), page.totalElements());
    }
}
