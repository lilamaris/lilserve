package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.CreateBlackoutScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateBlackoutScheduleResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * The reservation intake blackout as the web layer returns it. A blackout created in this request
 * has no update and no deletion timestamp, so those fields are null.
 *
 * @param blackoutScheduleId the blackout identifier
 * @param roomId the room owning the blocked slot
 * @param scheduleId the blocked recurring slot
 * @param reason the blackout reason
 * @param startsAt the inclusive intake start instant
 * @param endsAt the exclusive intake end instant
 * @param createdAt the creation time
 * @param updatedAt the last update time, null when never updated
 * @param deletedAt the deletion time, null when not deleted
 */
@Schema(
        description =
                "The reservation intake blackout as the web layer returns it. A blackout created in"
                        + " this request has no update and no deletion timestamp, so those fields are"
                        + " null.",
        example =
                """
                {"blackoutScheduleId":"00000000-0000-0000-0000-000000000005",\
                "roomId":"00000000-0000-0000-0000-000000000001",\
                "scheduleId":"00000000-0000-0000-0000-000000000003",\
                "reason":"facility maintenance",\
                "startsAt":"2026-01-02T00:00:00Z","endsAt":"2026-01-03T00:00:00Z",\
                "createdAt":"2026-01-01T00:00:00Z"}
                """)
public record BlackoutScheduleResponse(
        @Schema(
                        description = "blackout identifier",
                        type = "string",
                        format = "uuid",
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID blackoutScheduleId,
        @Schema(
                        description = "room owning the blocked slot",
                        type = "string",
                        format = "uuid",
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(
                        description = "blocked recurring slot",
                        type = "string",
                        format = "uuid",
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID scheduleId,
        @Schema(description = "blackout reason", accessMode = Schema.AccessMode.READ_ONLY)
                String reason,
        @Schema(
                        description = "inclusive intake start instant",
                        type = "string",
                        format = "date-time",
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant startsAt,
        @Schema(
                        description = "exclusive intake end instant",
                        type = "string",
                        format = "date-time",
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant endsAt,
        @Schema(
                        description = "creation time",
                        type = "string",
                        format = "date-time",
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
     * Maps a create result: the new blackout has no update and no deletion timestamp.
     *
     * @param result the created blackout
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static BlackoutScheduleResponse from(CreateBlackoutScheduleResult result) {
        return new BlackoutScheduleResponse(
                result.blackoutScheduleId().getValue(),
                result.roomId().getValue(),
                result.scheduleId().getValue(),
                result.reason(),
                result.startsAt(),
                result.endsAt(),
                result.createdAt(),
                null,
                null);
    }

    /**
     * Maps a change result with the blackout's change history.
     *
     * @param result the changed blackout
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static BlackoutScheduleResponse from(UpdateBlackoutScheduleResult result) {
        return new BlackoutScheduleResponse(
                result.blackoutScheduleId().getValue(),
                result.roomId().getValue(),
                result.scheduleId().getValue(),
                result.reason(),
                result.startsAt(),
                result.endsAt(),
                result.createdAt(),
                result.updatedAt(),
                result.deletedAt());
    }
}
