package com.lilamaris.lilserve.reservation.web.controller;

import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.web.ApiVersionConfiguration;
import com.lilamaris.lilserve.reservation.web.dto.CreateReservationRequest;
import com.lilamaris.lilserve.reservation.web.dto.OpenApiExamples;
import com.lilamaris.lilserve.reservation.web.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Maps reservation creation requests to the application use case. */
@RestController
@RequestMapping(path = "/api/v{version}/reservations", version = ApiVersionConfiguration.CURRENT)
@RequiredArgsConstructor
@Tag(name = "Reservation", description = "Seat reservations.")
public class ReservationController {
    private final CreateReservationUseCase createReservationUseCase;

    /**
     * Creates a reservation after validating the request body.
     *
     * @param request the requested seat, slots and service date
     * @return 201 with the created reservation
     */
    @Operation(
            operationId = "createReservation",
            summary = "Create a seat reservation",
            description =
                    "Secures every selected slot in one transaction. A blackout overlapping actual service intervals or an incomplete allocation rejects the whole reservation.")
    @ApiResponse(
            responseCode = "201",
            description = "created reservation",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(value = OpenApiExamples.RESERVATION)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "invalid body, identifiers, date, API version or seat belonging to another room",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "room, seat or selected slot does not exist or is unavailable",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description =
                    "room usage is blocked, slots cannot all be allocated or a persistence constraint is violated",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ReservationResponse.from(
                                createReservationUseCase.create(request.toCommand())));
    }
}
