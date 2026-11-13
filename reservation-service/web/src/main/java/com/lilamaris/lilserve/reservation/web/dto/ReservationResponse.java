package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A successfully created reservation with every requested slot secured.
 *
 * @param reservationId the reservation identifier
 * @param roomId the room identifier
 * @param seatId the seat identifier
 * @param scheduleIds the successfully reserved slot identifiers
 * @param reservationDate the service date
 * @param timeZone the stored application timezone
 * @param createdAt the creation instant
 */
@Schema(
        description = "A created reservation and its secured slots.",
        example = OpenApiExamples.RESERVATION)
public record ReservationResponse(
        @Schema(
                        description = "reservation identifier",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID reservationId,
        @Schema(
                        description = "room identifier",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID roomId,
        @Schema(
                        description = "seat identifier",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                UUID seatId,
        @Schema(
                        description = "successfully reserved slot identifiers",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Set<UUID> scheduleIds,
        @Schema(
                        description = "service date",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                LocalDate reservationDate,
        @Schema(
                        description = "stored application timezone",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                String timeZone,
        @Schema(
                        description = "creation instant",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        accessMode = Schema.AccessMode.READ_ONLY)
                Instant createdAt) {
    /**
     * Converts the application result into a JSON response.
     *
     * @param result the successfully created reservation
     * @return the response body
     */
    public static ReservationResponse from(CreateReservationResult result) {
        return new ReservationResponse(
                result.reservationId().getValue(),
                result.roomId().getValue(),
                result.seatId().getValue(),
                result.scheduleIds().stream().map(ScheduleId::getValue).collect(Collectors.toSet()),
                result.reservationDate(),
                result.timeZone(),
                result.createdAt());
    }
}
