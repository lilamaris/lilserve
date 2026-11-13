package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.result.ListAvailableSeatResult;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Seat identifiers available at lookup time for one room and service date.
 *
 * @param roomId the owning room identifier
 * @param targetDate the requested service date in the application timezone
 * @param availableSeatIds available seat identifiers in seat code and identifier order
 */
@Schema(
        description = "Available seats for the requested service date",
        example = OpenApiExamples.AVAILABLE_SEATS)
public record AvailableSeatResponse(
        @Schema(description = "Owning room identifier", requiredMode = Schema.RequiredMode.REQUIRED)
                UUID roomId,
        @Schema(description = "Requested service date", requiredMode = Schema.RequiredMode.REQUIRED)
                LocalDate targetDate,
        @Schema(
                        description = "Available seat identifiers, empty when none are available",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<UUID> availableSeatIds) {
    /**
     * Converts application identifiers into the UUID values published by the API.
     *
     * @param result the seat availability result
     * @return the response with seat order preserved
     */
    public static AvailableSeatResponse from(ListAvailableSeatResult result) {
        return new AvailableSeatResponse(
                result.roomId().getValue(),
                result.targetDate(),
                result.availableSeatIds().stream().map(SeatId::getValue).toList());
    }
}
