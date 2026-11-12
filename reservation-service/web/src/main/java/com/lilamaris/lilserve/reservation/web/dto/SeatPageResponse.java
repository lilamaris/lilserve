package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One page of seats with the offset metadata the web layer returns.
 *
 * <p>Every field is always present, so the document publishes all four as required. The items are
 * the seats of one room in code order, and an empty page is answered with an empty array.
 *
 * @param items the seats of this page
 * @param page the zero-based page number
 * @param size the maximum number of items per page
 * @param totalElements the number of matching seats across all pages
 */
@Schema(
        description =
                "One page of seats with offset metadata. An empty page is answered with an empty"
                        + " items array.",
        example = OpenApiExamples.SEAT_PAGE)
public record SeatPageResponse(
        @Schema(
                        description = "seats of this page, in code order",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<SeatResponse> items,
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
                        description = "number of matching seats across all pages",
                        type = "integer",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long totalElements) {

    /**
     * Maps a page of seat responses.
     *
     * @param page the page of mapped seats with its metadata
     * @return the response body
     * @throws NullPointerException if {@code page} is null
     */
    public static SeatPageResponse of(OffsetPage<SeatResponse> page) {
        return new SeatPageResponse(page.items(), page.page(), page.size(), page.totalElements());
    }
}
