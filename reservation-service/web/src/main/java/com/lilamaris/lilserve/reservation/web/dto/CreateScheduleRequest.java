package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The JSON body that creates one recurring room slot. The room is carried by the body, as a seat
 * carries its room, so the parent is not part of the path. The weekday and times are required, and
 * {@code endAt} must fall after {@code startAt}: the time range validates its own bounds when the
 * command is built, so an interval that ends on or before its start is answered as 400.
 *
 * @param roomId the room offering the slot, not null
 * @param dayOfWeek the ISO weekday the slot repeats on, not null
 * @param startAt the inclusive local start time, not null
 * @param endAt the exclusive local end time, not null
 */
@Schema(
        description = "The body that creates one recurring room slot.",
        example =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","dayOfWeek":"MONDAY",\
                "startAt":"09:00:00","endAt":"11:00:00"}
                """)
public record CreateScheduleRequest(
        @Schema(
                        description = "room offering the slot",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                UUID roomId,
        @Schema(
                        description = "ISO weekday the slot repeats on",
                        type = "string",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                DayOfWeek dayOfWeek,
        @Schema(
                        description = "inclusive local start time",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                LocalTime startAt,
        @Schema(
                        description = "exclusive local end time, after startAt",
                        type = "string",
                        format = "time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                LocalTime endAt) {

    /**
     * Converts this body to the create command. Required fields are checked by Bean Validation at
     * the web boundary; the interval ordering is checked by the time range the command builds.
     *
     * @return the create command
     * @throws NullPointerException if {@code roomId} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endAt} is not after {@code startAt}
     */
    public CreateScheduleCommand toCommand() {
        return CreateScheduleCommand.of(RoomId.of(roomId), startAt, endAt, dayOfWeek);
    }
}
