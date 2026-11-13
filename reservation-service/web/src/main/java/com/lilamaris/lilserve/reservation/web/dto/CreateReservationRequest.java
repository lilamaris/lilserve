package com.lilamaris.lilserve.reservation.web.dto;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A request to reserve one seat and all selected slots on one service date.
 *
 * @param roomId the room identifier
 * @param seatId the seat identifier
 * @param scheduleIds the non-empty selection of slot identifiers
 * @param reservationDate the service date in the application timezone
 */
@Schema(description = "Reserve one seat and all selected slots on one service date.")
public record CreateReservationRequest(
        @NotNull
                @Schema(
                        description = "room identifier",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                UUID roomId,
        @NotNull
                @Schema(
                        description = "seat identifier",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                UUID seatId,
        @NotEmpty
                @Schema(
                        description = "selected slot identifiers",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<@NotNull UUID> scheduleIds,
        @NotNull
                @Schema(
                        description = "service date in the application timezone",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                LocalDate reservationDate) {
    /**
     * Converts a validated HTTP body into the application command.
     *
     * @return the command with typed resource identifiers
     */
    public CreateReservationCommand toCommand() {
        return CreateReservationCommand.of(
                RoomId.of(roomId),
                SeatId.of(seatId),
                scheduleIds.stream().map(ScheduleId::of).collect(Collectors.toSet()),
                reservationDate);
    }
}
