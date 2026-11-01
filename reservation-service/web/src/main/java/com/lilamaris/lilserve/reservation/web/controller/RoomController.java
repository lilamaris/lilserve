package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.web.RoomExceptionHandler;
import com.lilamaris.lilserve.reservation.web.dto.CreateRoomRequest;
import com.lilamaris.lilserve.reservation.web.dto.RoomResponse;
import com.lilamaris.lilserve.reservation.web.dto.UpdateRoomCodeRequest;
import com.lilamaris.lilserve.reservation.web.dto.UpdateRoomMetadataRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The web driven adapter for rooms. Each endpoint maps one request to one room use case and returns
 * the result as JSON, so no business rule is implemented here.
 *
 * <p>Deletion is soft: delete records {@code deletedAt} and leaves the row, restore clears it. A
 * room that does not exist, or is not in the deletion state the operation requires, is reported as
 * 404 by {@link RoomExceptionHandler}.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(
        name = "Room",
        description = "Rooms: create, find, change metadata, change code, delete, restore.")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description =
                    "the body carried a value the domain rejected, or the identifier is not a UUID",
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
                    "the room does not exist, or it is not in the deletion state the operation"
                            + " requires",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Not"
                                                            + " Found\",\"status\":404,\"detail\":\"room"
                                                            + " does not exist.\"}")))
})
public class RoomController {
    private final CreateRoomUseCase createRoomUseCase;
    private final FindRoomUseCase findRoomUseCase;
    private final UpdateRoomMetadataUseCase updateRoomMetadataUseCase;
    private final UpdateRoomCodeUseCase updateRoomCodeUseCase;
    private final DeleteRoomUseCase deleteRoomUseCase;
    private final RestoreRoomUseCase restoreRoomUseCase;

    /**
     * Creates a room from the request body.
     *
     * @param request the create body, validated before the use case runs
     * @return 201 with the created room
     */
    @Operation(
            summary = "Create a room",
            description =
                    "Creates a room from a code, a name, and a description. The code is unique, so"
                            + " a code that is already used is reported as 409 by persistence.")
    @ApiResponse(
            responseCode = "201",
            description = "created room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomResponse.class)))
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoomResponse.from(createRoomUseCase.create(request.toCommand())));
    }

    /**
     * Finds one room by identifier, excluding deleted rooms.
     *
     * @param roomId the room identifier
     * @return 200 with the found room
     */
    @Operation(
            summary = "Find a room",
            description =
                    "Finds one room that is not deleted. A deleted room is answered as 404, because"
                            + " it is not in the state this operation acts on.")
    @ApiResponse(
            responseCode = "200",
            description = "found room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomResponse.class)))
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> find(
            @Parameter(
                            description = "room identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("roomId")
                    UUID roomId) {
        return ResponseEntity.ok(RoomResponse.from(findRoomUseCase.find(FindRoomQuery.of(roomId))));
    }

    /**
     * Changes a room's name and description.
     *
     * @param roomId the room identifier
     * @param request the new metadata, validated before the use case runs
     * @return 200 with the room that has the new metadata
     */
    @Operation(
            summary = "Change a room's name and description",
            description =
                    "Changes the metadata of one room. The room must not be deleted, otherwise the"
                            + " answer is 404.")
    @ApiResponse(
            responseCode = "200",
            description = "room with the new metadata",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomResponse.class)))
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateMetadata(
            @Parameter(
                            description = "room identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("roomId")
                    UUID roomId,
            @Valid @RequestBody UpdateRoomMetadataRequest request) {
        return ResponseEntity.ok(
                RoomResponse.from(
                        updateRoomMetadataUseCase.updateMetadata(request.toCommand(roomId))));
    }

    /**
     * Changes a room's code.
     *
     * @param roomId the room identifier
     * @param request the new code, validated before the use case runs
     * @return 200 with the room that has the new code
     */
    @Operation(
            summary = "Change a room's code",
            description =
                    "Changes the code of one room. The code is unique in the database, so a code"
                            + " another room uses is answered as 409.")
    @ApiResponse(
            responseCode = "200",
            description = "room with the new code",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "the code is already used by another room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    "{\"type\":\"about:blank\",\"title\":\"Conflict\","
                                                            + "\"status\":409,\"detail\":\"room code is"
                                                            + " already used.\"}")))
    @PatchMapping("/{roomId}/code")
    public ResponseEntity<RoomResponse> updateCode(
            @Parameter(
                            description = "room identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("roomId")
                    UUID roomId,
            @Valid @RequestBody UpdateRoomCodeRequest request) {
        return ResponseEntity.ok(
                RoomResponse.from(updateRoomCodeUseCase.updateCode(request.toCommand(roomId))));
    }

    /**
     * Deletes a room by recording its deletion time. The row is kept, so 204 has no body.
     *
     * @param roomId the room identifier
     * @return 204 without a body
     */
    @Operation(
            summary = "Delete a room",
            description =
                    "Soft delete: the deletion time is recorded and the row is kept, so the answer"
                            + " has no body. The room must not be deleted already, otherwise the"
                            + " answer is 404.")
    @ApiResponse(responseCode = "204", description = "room deleted, no body")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> delete(
            @Parameter(
                            description = "room identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("roomId")
                    UUID roomId) {
        deleteRoomUseCase.delete(DeleteRoomCommand.of(roomId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a deleted room by clearing its deletion time.
     *
     * @param roomId the room identifier
     * @return 200 with the restored room
     */
    @Operation(
            summary = "Restore a deleted room",
            description =
                    "Clears the deletion time of a deleted room. A room that is not deleted is"
                            + " answered as 404, because it is not in the state this operation acts"
                            + " on.")
    @ApiResponse(
            responseCode = "200",
            description = "restored room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomResponse.class)))
    @PostMapping("/{roomId}/restore")
    public ResponseEntity<RoomResponse> restore(
            @Parameter(
                            description = "room identifier",
                            schema = @Schema(type = "string", format = "uuid"))
                    @PathVariable("roomId")
                    UUID roomId) {
        return ResponseEntity.ok(
                RoomResponse.from(restoreRoomUseCase.restore(RestoreRoomCommand.of(roomId))));
    }
}
