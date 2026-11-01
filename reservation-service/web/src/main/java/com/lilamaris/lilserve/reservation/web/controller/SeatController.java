package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.web.ApiVersionConfiguration;
import com.lilamaris.lilserve.reservation.web.SeatExceptionHandler;
import com.lilamaris.lilserve.reservation.web.dto.CreateSeatRequest;
import com.lilamaris.lilserve.reservation.web.dto.SeatResponse;
import com.lilamaris.lilserve.reservation.web.dto.UpdateSeatCodeRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The web driven adapter for seats. Each endpoint maps one request to one seat use case and returns
 * the result as JSON, so no business rule is implemented here.
 *
 * <p>A seat belongs to one room, so the create body carries the room identifier and the use case
 * loads that room. Deletion is soft: delete records {@code deletedAt} and leaves the row, restore
 * clears it. A seat that does not exist, or is not in the deletion state the operation requires, is
 * reported as 404 by {@link SeatExceptionHandler}.
 *
 * <p>The path carries the API version as a segment, and the mapping declares version {@code 1}, so
 * a request for a version this layer does not declare, and a request with no version at that
 * position, is answered with 400 by {@link ApiVersionConfiguration} and no use case runs.
 */
@RestController
@RequestMapping(path = "/api/{version}/seats", version = ApiVersionConfiguration.CURRENT)
@RequiredArgsConstructor
@Tag(name = "Seat", description = "Seats: create, find, change code, delete, restore.")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description =
                    "the body carried a value the domain rejected, the identifier is not a UUID,"
                            + " the request carries no API version, or a version this layer does not"
                            + " declare",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Bad"
                                                            + " Request\",\"status\":400,\"detail\":\"code must not be"
                                                            + " blank.\"}"))),
    @ApiResponse(
            responseCode = "404",
            description =
                    "the seat does not exist, the room the seat belongs to does not exist, or the"
                            + " target is not in the deletion state the operation requires",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Not"
                                                            + " Found\",\"status\":404,\"detail\":\"seat"
                                                            + " does not exist.\"}")))
})
public class SeatController {
    private final CreateSeatUseCase createSeatUseCase;
    private final FindSeatUseCase findSeatUseCase;
    private final UpdateSeatCodeUseCase updateSeatCodeUseCase;
    private final DeleteSeatUseCase deleteSeatUseCase;
    private final RestoreSeatUseCase restoreSeatUseCase;

    /**
     * Creates a seat in the room carried by the request body.
     *
     * @param request the create body, validated before the use case runs
     * @return 201 with the created seat
     */
    @Operation(
            summary = "Create a seat in a room",
            description =
                    "Creates a seat in a room that exists and is not deleted. The code is unique, so"
                            + " a code that is already used is reported as 409 by persistence.")
    @ApiResponse(
            responseCode = "201",
            description = "created seat",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SeatResponse.class)))
    @PostMapping
    public ResponseEntity<SeatResponse> create(@Valid @RequestBody CreateSeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SeatResponse.from(createSeatUseCase.create(request.toCommand())));
    }

    /**
     * Finds one seat by identifier, excluding deleted seats.
     *
     * @param seatId the seat identifier
     * @return 200 with the found seat
     */
    @Operation(
            summary = "Find a seat",
            description =
                    "Finds one seat that is not deleted. A deleted seat is answered as 404, because"
                            + " it is not in the state this operation acts on.")
    @ApiResponse(
            responseCode = "200",
            description = "found seat",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SeatResponse.class)))
    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> find(
            @Parameter(
                            description = "seat identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("seatId")
                    UUID seatId) {
        return ResponseEntity.ok(SeatResponse.from(findSeatUseCase.find(FindSeatQuery.of(seatId))));
    }

    /**
     * Changes a seat's code.
     *
     * @param seatId the seat identifier
     * @param request the new code, validated before the use case runs
     * @return 200 with the seat that has the new code
     */
    @Operation(
            summary = "Change a seat's code",
            description =
                    "Changes the code of one seat. The seat must not be deleted, otherwise the answer"
                            + " is 404.")
    @ApiResponse(
            responseCode = "200",
            description = "seat with the new code",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SeatResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "the code is already used by another seat",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Conflict\","
                                                            + "\"status\":409,\"detail\":\"seat code is"
                                                            + " already used.\"}")))
    @PatchMapping("/{seatId}/code")
    public ResponseEntity<SeatResponse> updateCode(
            @Parameter(
                            description = "seat identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("seatId")
                    UUID seatId,
            @Valid @RequestBody UpdateSeatCodeRequest request) {
        return ResponseEntity.ok(
                SeatResponse.from(updateSeatCodeUseCase.updateCode(request.toCommand(seatId))));
    }

    /**
     * Deletes a seat by recording its deletion time. The row is kept, so 204 has no body.
     *
     * @param seatId the seat identifier
     * @return 204 without a body
     */
    @Operation(
            summary = "Delete a seat",
            description =
                    "Soft delete: the deletion time is recorded and the row is kept, so the answer has"
                            + " no body. The seat must not be deleted already, otherwise the answer is"
                            + " 404.")
    @ApiResponse(responseCode = "204", description = "seat deleted, no body")
    @DeleteMapping("/{seatId}")
    public ResponseEntity<Void> delete(
            @Parameter(
                            description = "seat identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("seatId")
                    UUID seatId) {
        deleteSeatUseCase.delete(DeleteSeatCommand.of(seatId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a deleted seat by clearing its deletion time.
     *
     * @param seatId the seat identifier
     * @return 200 with the restored seat
     */
    @Operation(
            summary = "Restore a deleted seat",
            description =
                    "Clears the deletion time of a deleted seat. A seat that is not deleted is"
                            + " answered as 404, because it is not in the state this operation acts on.")
    @ApiResponse(
            responseCode = "200",
            description = "restored seat",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SeatResponse.class)))
    @PostMapping("/{seatId}/restore")
    public ResponseEntity<SeatResponse> restore(
            @Parameter(
                            description = "seat identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("seatId")
                    UUID seatId) {
        return ResponseEntity.ok(
                SeatResponse.from(restoreSeatUseCase.restore(RestoreSeatCommand.of(seatId))));
    }
}
