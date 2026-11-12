package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * The seat as the web layer returns it. A seat created in this request has no update and no
 * deletion timestamp, so those fields are null.
 *
 * @param seatId the seat identifier
 * @param roomId the room the seat belongs to
 * @param code the seat code
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
@Schema(
        description =
                "The seat as the web layer returns it. A seat created in this request has no update"
                        + " and no deletion timestamp, so those fields are null.",
        example = OpenApiExamples.SEAT)
public record SeatResponse(
        @Schema(
                        description = "seat identifier",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID seatId,
        @Schema(
                        description = "room the seat belongs to",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(
                        description = "seat code",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                String code,
        @Schema(
                        description = "creation time",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant createdAt,
        @Schema(
                        description = "last update time, null when never updated",
                        type = "string",
                        format = "date-time",
                        nullable = true,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant updatedAt,
        @Schema(
                        description = "deletion time, null when not deleted",
                        type = "string",
                        format = "date-time",
                        nullable = true,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant deletedAt) {

    /**
     * Maps a create result: the new seat has no update and no deletion timestamp.
     *
     * @param result the created seat
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static SeatResponse from(CreateSeatResult result) {
        return new SeatResponse(
                result.seatId().getValue(),
                result.roomId().getValue(),
                result.code(),
                result.createdAt(),
                null,
                null);
    }

    /**
     * Maps a find result with the seat's change history.
     *
     * @param result the found seat
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static SeatResponse from(FindSeatResult result) {
        return new SeatResponse(
                result.seatId().getValue(),
                result.roomId().getValue(),
                result.code(),
                result.createdAt(),
                result.updatedAt(),
                result.deletedAt());
    }

    /**
     * Maps a change result with the seat's change history.
     *
     * @param result the changed seat
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static SeatResponse from(UpdateSeatResult result) {
        return new SeatResponse(
                result.seatId().getValue(),
                result.roomId().getValue(),
                result.code(),
                result.createdAt(),
                result.updatedAt(),
                result.deletedAt());
    }
}
