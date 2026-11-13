package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * One recurring slot of a room with the room usage blackouts active at the requested instant. When
 * the room is available at that instant, each slot carries an empty {@code blackouts} array, so a
 * consumer never has to distinguish a missing field from an available room.
 *
 * @param scheduleId the slot identifier
 * @param dayOfWeek the ISO weekday the slot repeats on
 * @param startAt the inclusive local start time
 * @param endAt the exclusive local end time
 * @param blackouts the blackouts active at the requested instant, empty when the room is available
 */
@Schema(
        description =
                "One recurring slot with the blackouts active at the requested instant. An available room"
                        + " has an empty blackouts array on each slot.",
        example = OpenApiExamples.SCHEDULE_SLOT)
public record DayScheduleResponse(
        @Schema(
                        description = "slot identifier",
                        type = "string",
                        format = "uuid",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID scheduleId,
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
                        description =
                                "blackouts active at the requested instant, empty when the room is"
                                        + " available",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                List<ScheduleBlackoutResponse> blackouts) {

    /**
     * Maps one listed slot and its active blackouts.
     *
     * @param daySchedule the slot with its active blackouts
     * @return the nested slot body
     * @throws NullPointerException if {@code daySchedule} is null
     */
    public static DayScheduleResponse from(DaySchedule daySchedule) {
        return new DayScheduleResponse(
                daySchedule.scheduleId().getValue(),
                daySchedule.dayOfWeek(),
                daySchedule.period().startAt(),
                daySchedule.period().endAt(),
                daySchedule.blackouts().stream().map(ScheduleBlackoutResponse::from).toList());
    }
}
