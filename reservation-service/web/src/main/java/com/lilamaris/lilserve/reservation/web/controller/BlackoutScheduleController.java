package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.reservation.application.port.in.CreateBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutSchedulePeriodUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutScheduleReasonUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.web.ApiVersionConfiguration;
import com.lilamaris.lilserve.reservation.web.BlackoutScheduleExceptionHandler;
import com.lilamaris.lilserve.reservation.web.dto.BlackoutScheduleResponse;
import com.lilamaris.lilserve.reservation.web.dto.CreateBlackoutScheduleRequest;
import com.lilamaris.lilserve.reservation.web.dto.OpenApiExamples;
import com.lilamaris.lilserve.reservation.web.dto.UpdateBlackoutSchedulePeriodRequest;
import com.lilamaris.lilserve.reservation.web.dto.UpdateBlackoutScheduleReasonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maps HTTP requests for room usage blackouts to application use cases and returns their results as
 * JSON.
 *
 * <p>A blackout restricts physical room usage over an absolute interval, and never cancels existing
 * reservations. Creation and both changes answer with the stored blackout, so the identifier the
 * change and delete endpoints require comes from the creation that produced it. Deletion is soft,
 * so 204 has no body. {@link BlackoutScheduleExceptionHandler} maps application failures to HTTP
 * problem details. {@link ApiVersionConfiguration} resolves the API version from the request path.
 */
@RestController
@RequestMapping(path = "/api/v{version}/blackouts", version = ApiVersionConfiguration.CURRENT)
@RequiredArgsConstructor
@Tag(
        name = "Blackout",
        description =
                "Reservation usage blackouts: create, change interval, change reason, delete.")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description =
                    "the body carried a value the domain rejected, an interval ended on or before"
                            + " its start, the identifier is not a UUID, the request carries no API"
                            + " version, or a version this layer does not declare",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Bad"
                                                            + " Request\",\"status\":400,\"detail\":\"must be after"
                                                            + " 2026-01-03T00:00:00Z.\"}"))),
    @ApiResponse(
            responseCode = "404",
            description =
                    "the blackout does not exist, it is already deleted, the slot it belongs to does"
                            + " not exist, or the room does not exist",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Not"
                                                            + " Found\",\"status\":404,\"detail\":\"blackout schedule"
                                                            + " does not exist.\"}")))
})
public class BlackoutScheduleController {
    private final CreateBlackoutScheduleUseCase createBlackoutScheduleUseCase;
    private final UpdateBlackoutSchedulePeriodUseCase updateBlackoutSchedulePeriodUseCase;
    private final UpdateBlackoutScheduleReasonUseCase updateBlackoutScheduleReasonUseCase;
    private final DeleteBlackoutScheduleUseCase deleteBlackoutScheduleUseCase;

    /**
     * Restricts usage of the room carried by the request body.
     *
     * @param request the create body, validated before the use case runs
     * @return 201 with the created blackout
     */
    @Operation(
            operationId = "createBlackoutSchedule",
            summary = "Restrict physical room usage",
            description =
                    "Blocks reservations whose actual service intervals overlap the requested absolute"
                            + " period, regardless of booking time. The restriction covers every slot"
                            + " in the room; scheduleId records the registration context only."
                            + " Overlapping blackouts are allowed and"
                            + " existing reservations are preserved. The answer carries the created"
                            + " blackout identifier, which the change and delete endpoints require."
                            + " The slot must belong to the room in the body and be active: a slot"
                            + " that is missing is answered as 404, and a slot another room owns is"
                            + " answered as 409.")
    @ApiResponse(
            responseCode = "201",
            description = "created blackout",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BlackoutScheduleResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.BLACKOUT)))
    @ApiResponse(
            responseCode = "409",
            description = "the slot belongs to another room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Conflict\","
                                                            + "\"status\":409,\"detail\":\"schedule does not belong"
                                                            + " to the room.\"}")))
    @PostMapping
    public ResponseEntity<BlackoutScheduleResponse> create(
            @Valid @RequestBody CreateBlackoutScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        BlackoutScheduleResponse.from(
                                createBlackoutScheduleUseCase.create(request.toCommand())));
    }

    /**
     * Replaces a blackout's usage interval.
     *
     * @param blackoutScheduleId the blackout to change
     * @param request the replacement interval, validated before the use case runs
     * @return 200 with the blackout that has the new interval
     */
    @Operation(
            operationId = "updateBlackoutSchedulePeriod",
            summary = "Replace a blackout's usage interval",
            description =
                    "New usage decides against either the preceding interval or the committed"
                            + " replacement, never a mix. Existing reservations are preserved. The"
                            + " blackout must not be deleted, otherwise the answer is 404.")
    @ApiResponse(
            responseCode = "200",
            description = "blackout with the new interval",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BlackoutScheduleResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.BLACKOUT)))
    @PatchMapping("/{blackoutScheduleId}/period")
    public ResponseEntity<BlackoutScheduleResponse> updatePeriod(
            @Parameter(
                            description = "blackout identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("blackoutScheduleId")
                    UUID blackoutScheduleId,
            @Valid @RequestBody UpdateBlackoutSchedulePeriodRequest request) {
        return ResponseEntity.ok(
                BlackoutScheduleResponse.from(
                        updateBlackoutSchedulePeriodUseCase.updateRange(
                                request.toCommand(blackoutScheduleId))));
    }

    /**
     * Replaces a blackout's reason.
     *
     * @param blackoutScheduleId the blackout to change
     * @param request the new reason, validated before the use case runs
     * @return 200 with the blackout that has the new reason
     */
    @Operation(
            operationId = "updateBlackoutScheduleReason",
            summary = "Replace a blackout's reason",
            description =
                    "Changes the explanation only, so usage decisions do not move. The blackout must"
                            + " not be deleted, otherwise the answer is 404.")
    @ApiResponse(
            responseCode = "200",
            description = "blackout with the new reason",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BlackoutScheduleResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.BLACKOUT)))
    @PatchMapping("/{blackoutScheduleId}/reason")
    public ResponseEntity<BlackoutScheduleResponse> updateReason(
            @Parameter(
                            description = "blackout identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("blackoutScheduleId")
                    UUID blackoutScheduleId,
            @Valid @RequestBody UpdateBlackoutScheduleReasonRequest request) {
        return ResponseEntity.ok(
                BlackoutScheduleResponse.from(
                        updateBlackoutScheduleReasonUseCase.updateReason(
                                request.toCommand(blackoutScheduleId))));
    }

    /**
     * Deletes a blackout by recording its deletion time. The row is kept, so 204 has no body.
     *
     * @param blackoutScheduleId the blackout to delete
     * @return 204 without a body
     */
    @Operation(
            operationId = "deleteBlackoutSchedule",
            summary = "Delete a blackout",
            description =
                    "Soft delete: the deletion time is recorded and the row is kept, so the answer has"
                            + " no body. Existing reservations are preserved. The blackout must not be"
                            + " deleted already, otherwise the answer is 404.")
    @ApiResponse(responseCode = "204", description = "blackout deleted, no body")
    @DeleteMapping("/{blackoutScheduleId}")
    public ResponseEntity<Void> delete(
            @Parameter(
                            description = "blackout identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("blackoutScheduleId")
                    UUID blackoutScheduleId) {
        deleteBlackoutScheduleUseCase.delete(
                DeleteBlackoutScheduleCommand.of(BlackoutScheduleId.of(blackoutScheduleId)));
        return ResponseEntity.noContent().build();
    }
}
