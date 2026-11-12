package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.ListScheduleResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * The recurring slots of one room with the reservation intake blackouts active at the requested
 * instant.
 *
 * <p>Both fields are always present, so the document publishes both as required. A room that offers
 * no slot is answered with an empty {@code schedules} array, and the slots carry no guaranteed
 * weekday ordering.
 *
 * @param roomId the room the slots belong to
 * @param schedules the room slots with their active blackouts
 */
@Schema(
        description =
                "The slots of one room with the blackouts active at the requested instant. A room"
                        + " with no slot is answered with an empty schedules array.",
        example = OpenApiExamples.SCHEDULE_LIST)
public record ScheduleListResponse(
        @Schema(
                        description = "room offering the slots",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(
                        description =
                                "room slots with their active blackouts, in no guaranteed order",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                List<DayScheduleResponse> schedules) {

    /**
     * Maps a slot list result.
     *
     * @param result the listed slots of the room
     * @return the response body
     * @throws NullPointerException if {@code result} is null
     */
    public static ScheduleListResponse from(ListScheduleResult result) {
        return new ScheduleListResponse(
                result.roomId().getValue(),
                result.schedules().stream().map(DayScheduleResponse::from).toList());
    }
}
