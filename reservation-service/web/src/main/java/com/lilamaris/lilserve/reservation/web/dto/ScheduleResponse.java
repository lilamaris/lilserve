package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The recurring room slot as the web layer returns it. A slot carries no update and no deletion
 * timestamp: a replacement retires the original slot and is offered under a new identifier, so the
 * answer carries the creation time only.
 *
 * @param scheduleId the slot identifier
 * @param roomId the room offering the slot
 * @param dayOfWeek the ISO weekday the slot repeats on
 * @param startAt the inclusive local start time
 * @param endAt the exclusive local end time
 * @param createdAt the creation time
 */
@Schema(
        description =
                "The recurring room slot as the web layer returns it. A replacement slot carries a"
                        + " new identifier, so a retired slot is never answered under this shape.",
        example = OpenApiExamples.SCHEDULE)
public record ScheduleResponse(
        @Schema(
                        description = "slot identifier",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID scheduleId,
        @Schema(
                        description = "room offering the slot",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(
                        description = "ISO weekday the slot repeats on",
                        type = "string",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                DayOfWeek dayOfWeek,
        @Schema(
                        description = "inclusive local start time",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                LocalTime startAt,
        @Schema(
                        description = "exclusive local end time",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                LocalTime endAt,
        @Schema(
                        description = "creation time",
                        type = "string",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant createdAt) {

    /**
     * Maps a create result.
     *
     * @param result the created slot
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static ScheduleResponse from(CreateScheduleResult result) {
        return new ScheduleResponse(
                result.scheduleId().getValue(),
                result.roomId().getValue(),
                result.dayOfWeek(),
                result.startAt(),
                result.endAt(),
                result.createdAt());
    }

    /**
     * Maps a replacement result: the slot is new, so its identifier differs from the retired
     * slot's.
     *
     * @param result the replacement slot
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static ScheduleResponse from(UpdateScheduleResult result) {
        return new ScheduleResponse(
                result.scheduleId().getValue(),
                result.roomId().getValue(),
                result.dayOfWeek(),
                result.startAt(),
                result.endAt(),
                result.createdAt());
    }
}
