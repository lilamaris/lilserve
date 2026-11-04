package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.*;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CreateReservationService implements CreateReservationUseCase {
    private final ReservationSavePort reservationSavePort;
    private final ReserveSchedulePort reserveSchedulePort;
    private final RoomFindPort roomFindPort;
    private final SeatFindPort seatFindPort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CreateReservationResult create(CreateReservationCommand command) {
        var now = clock.instant();

        var room =
                roomFindPort
                        .findNotDeletedById(command.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var seat =
                seatFindPort
                        .findNotDeletedById(command.seatId())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        if (!seat.getRoomId().equals(room.getId()))
            throw new IllegalArgumentException("seat must be belong to room.");

        var timezone = clock.getZone();

        var reservationId = uuidGenerator.generate();
        var reservation =
                Reservation.of(
                        reservationId,
                        room.getId().getValue(),
                        seat.getId().getValue(),
                        command.reservationDate(),
                        timezone.getId(),
                        now);

        reservationSavePort.save(reservation);

        var reserveStatus = reserveSchedulePort.reserve(reservation.getId(), command.scheduleIds());

        if (reserveStatus == ReserveScheduleStatus.FAILED) {
            throw new IllegalStateException("failed to reserve schedules.");
        }
        return CreateReservationResult.from(reservation, command.scheduleIds());
    }
}
