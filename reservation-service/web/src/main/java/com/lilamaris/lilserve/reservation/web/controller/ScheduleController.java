package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListScheduleQuery;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.web.ApiVersionConfiguration;
import com.lilamaris.lilserve.reservation.web.ScheduleExceptionHandler;
import com.lilamaris.lilserve.reservation.web.dto.CreateScheduleRequest;
import com.lilamaris.lilserve.reservation.web.dto.CreateSchedulesRequest;
import com.lilamaris.lilserve.reservation.web.dto.OpenApiExamples;
import com.lilamaris.lilserve.reservation.web.dto.ScheduleListResponse;
import com.lilamaris.lilserve.reservation.web.dto.ScheduleResponse;
import com.lilamaris.lilserve.reservation.web.dto.UpdateScheduleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maps HTTP requests for recurring room slots to application use cases and returns their results as
 * JSON.
 *
 * <p>Slot definitions are append-only, so a change retires the original slot and offers a
 * replacement under a new identifier: {@code PUT} answers with that new slot, and the identifier
 * given in the path stops being offered. Retirement is soft, so 204 has no body. {@link
 * ScheduleExceptionHandler} maps application failures to HTTP problem details. {@link
 * ApiVersionConfiguration} resolves the API version from the request path.
 *
 * <p>The read operation is a room-scoped list: it answers the room's slots together with the room
 * usage restrictions active at the instant the caller asks about. The same room restrictions are
 * reported on every slot. This status snapshot does not determine date-specific booking
 * availability; the available-seat endpoint compares actual service intervals.
 */
@RestController
@RequestMapping(path = "/api/v{version}/schedules", version = ApiVersionConfiguration.CURRENT)
@RequiredArgsConstructor
@Tag(
        name = "Schedule",
        description =
                "Recurring room slots: list one room's slots with their active blackouts, create,"
                        + " create in batch, replace, retire.")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description =
                    "the body carried a value the domain rejected, an interval ended on or before"
                            + " its start, the identifier is not a UUID, a list query omits roomId"
                            + " or now, now is not an instant, the request carries no API version,"
                            + " or a version this layer does not declare",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Bad"
                                                            + " Request\",\"status\":400,\"detail\":\"endAt must be"
                                                            + " after startAt.\"}"))),
    @ApiResponse(
            responseCode = "404",
            description = "the slot does not exist, it is already retired, or the room is missing",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Not"
                                                            + " Found\",\"status\":404,\"detail\":\"schedule does not"
                                                            + " exist.\"}")))
})
public class ScheduleController {
    private final CreateScheduleUseCase createScheduleUseCase;
    private final ListScheduleUseCase listScheduleUseCase;
    private final UpdateScheduleUseCase updateScheduleUseCase;
    private final DeleteScheduleUseCase deleteScheduleUseCase;

    /**
     * Creates one recurring slot in the room carried by the request body.
     *
     * @param request the create body, validated before the use case runs
     * @return 201 with the created slot
     */
    @Operation(
            operationId = "createSchedule",
            summary = "Create one recurring slot in a room",
            description =
                    "Creates one recurring slot in a room that exists and is not deleted. The slot"
                            + " repeats on the requested weekday with the requested local time range.")
    @ApiResponse(
            responseCode = "201",
            description = "created slot",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.SCHEDULE)))
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleResponse.from(createScheduleUseCase.create(request.toCommand())));
    }

    /**
     * Creates the requested recurring slots atomically in the room carried by the request body.
     *
     * @param request the batch create body, validated before the use case runs
     * @return 201 with the created slots, in no guaranteed weekday order
     */
    @Operation(
            operationId = "createSchedules",
            summary = "Create recurring slots in a room as one batch",
            description =
                    "Creates the requested slots in one transaction, so a failure leaves none of"
                            + " them persisted. The answer lists every created slot and has no"
                            + " guaranteed weekday ordering.")
    @ApiResponse(
            responseCode = "201",
            description = "created slots",
            content =
                    @Content(
                            mediaType = "application/json",
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    ScheduleResponse.class)),
                            examples = @ExampleObject(value = OpenApiExamples.SCHEDULE_BATCH)))
    @PostMapping("/batch")
    public ResponseEntity<List<ScheduleResponse>> createBatch(
            @Valid @RequestBody CreateSchedulesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        createScheduleUseCase.create(request.toCommand()).schedules().stream()
                                .map(ScheduleResponse::from)
                                .toList());
    }

    /**
     * Lists the recurring slots of one room with the room usage blackouts active at the requested
     * instant.
     *
     * @param roomId the room whose slots to list
     * @param now the usage instant whose active blackouts are reported
     * @return 200 with the room slots and their active blackouts, empty when the room offers none
     */
    @Operation(
            operationId = "listSchedules",
            summary = "List one room's recurring slots with their active blackouts",
            description =
                    "Lists the slots a room that exists and is not deleted offers. Each slot carries"
                            + " the room usage restrictions active at the requested instant. The same"
                            + " restrictions apply to every slot; an available room has empty blackout"
                            + " arrays. This is a status snapshot, not date-specific booking availability. A"
                            + " room with no slot is answered with an empty schedules array. Ordering"
                            + " is not guaranteed. A missing or deleted room is answered as 404.")
    @ApiResponse(
            responseCode = "200",
            description = "room slots with their active blackouts",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleListResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.SCHEDULE_LIST)))
    @GetMapping
    public ResponseEntity<ScheduleListResponse> list(
            @Parameter(
                            description = "room whose slots to list",
                            schema = @Schema(type = "string", format = "uuid"))
                    @RequestParam("roomId")
                    UUID roomId,
            @Parameter(
                            description = "instant whose active blackouts are reported",
                            schema = @Schema(type = "string", format = "date-time"))
                    @RequestParam("now")
                    Instant now) {
        var result = listScheduleUseCase.list(ListScheduleQuery.of(RoomId.of(roomId), now));
        return ResponseEntity.ok(ScheduleListResponse.from(result));
    }

    /**
     * Replaces a slot with the requested weekday and time range.
     *
     * @param scheduleId the slot to replace
     * @param request the replacement definition, validated before the use case runs
     * @return 200 with the replacement slot and its new identifier
     */
    @Operation(
            operationId = "updateSchedule",
            summary = "Replace a recurring slot",
            description =
                    "Slot definitions are append-only, so the times of the original slot are never"
                            + " edited in place: the original is retired and the replacement is"
                            + " offered under a new identifier under the same room. Allocations"
                            + " already made against the original keep their reference to it. The"
                            + " slot must be active, otherwise the answer is 404.")
    @ApiResponse(
            responseCode = "200",
            description = "replacement slot",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.SCHEDULE)))
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> update(
            @Parameter(
                            description = "slot identifier to replace",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("scheduleId")
                    UUID scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request) {
        return ResponseEntity.ok(
                ScheduleResponse.from(updateScheduleUseCase.update(request.toCommand(scheduleId))));
    }

    /**
     * Retires a slot so that it is no longer offered for new allocations. The row is kept, so 204
     * has no body.
     *
     * @param scheduleId the slot to retire
     * @return 204 without a body
     */
    @Operation(
            operationId = "deleteSchedule",
            summary = "Retire a recurring slot",
            description =
                    "Soft delete: the retirement time is recorded and the row is kept, so the answer"
                            + " has no body. Only new allocations stop; existing allocations are not"
                            + " cancelled. The slot must be active, otherwise the answer is 404.")
    @ApiResponse(responseCode = "204", description = "slot retired, no body")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            @Parameter(
                            description = "slot identifier to retire",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("scheduleId")
                    UUID scheduleId) {
        deleteScheduleUseCase.delete(ScheduleId.of(scheduleId));
        return ResponseEntity.noContent().build();
    }
}
