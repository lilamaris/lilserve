package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.web.ApiVersionConfiguration;
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
import jakarta.annotation.Nullable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maps HTTP requests for rooms to application use cases and returns their results as JSON.
 *
 * <p>Deletion is soft: deletion records {@code deletedAt}, and restoration clears it. {@link
 * RoomExceptionHandler} maps application failures to HTTP problem details. {@link
 * ApiVersionConfiguration} resolves the API version from the request path.
 */
@RestController
@RequestMapping(path = "/api/{version}/rooms", version = ApiVersionConfiguration.CURRENT)
@RequiredArgsConstructor
@Tag(
        name = "Room",
        description = "Rooms: create, find, list, change metadata, change code, delete, restore.")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description =
                    "the body or pagination carried a rejected value, the identifier is not a UUID,"
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
    private final ListRoomUseCase listRoomUseCase;
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
     * Lists non-deleted rooms in code order with offset page metadata.
     *
     * @param name optional case-insensitive literal room name fragment
     * @param code optional case-insensitive literal room code fragment
     * @param page the zero-based page number, default 0
     * @param size the positive page size, default 20; page times size must fit in a signed integer
     * @return 200 with room responses and the page number, size, and matching total
     */
    @Operation(
            summary = "List rooms",
            description =
                    "Lists non-deleted rooms in code order. Name and code filters are literal, case-insensitive substrings combined with AND. Blank filters are ignored. An empty or out-of-range page returns 200 with an empty items array. Page times size must not exceed 2147483647.")
    @ApiResponse(responseCode = "200", description = "Paginated room list with totalElements")
    @GetMapping
    public ResponseEntity<OffsetPage<RoomResponse>> list(
            @Parameter(description = "Optional room name fragment")
                    @RequestParam(name = "name", required = false)
                    @Nullable
                    String name,
            @Parameter(description = "Optional room code fragment")
                    @RequestParam(name = "code", required = false)
                    @Nullable
                    String code,
            @Parameter(
                            description = "Zero-based page number",
                            schema = @Schema(minimum = "0", defaultValue = "0"))
                    @RequestParam(name = "page", defaultValue = "0")
                    int page,
            @Parameter(
                            description = "Page size",
                            schema = @Schema(minimum = "1", defaultValue = "20"))
                    @RequestParam(name = "size", defaultValue = "20")
                    int size) {
        var query = ListRoomQuery.of(name, code, OffsetPageRequest.of(page, size));
        var result = listRoomUseCase.list(query);
        var items = result.items().stream().map(RoomResponse::from).toList();
        return ResponseEntity.ok(
                OffsetPage.of(items, result.page(), result.size(), result.totalElements()));
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
        deleteRoomUseCase.delete(DeleteRoomCommand.of(RoomId.of(roomId)));
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
                RoomResponse.from(
                        restoreRoomUseCase.restore(RestoreRoomCommand.of(RoomId.of(roomId)))));
    }
}
