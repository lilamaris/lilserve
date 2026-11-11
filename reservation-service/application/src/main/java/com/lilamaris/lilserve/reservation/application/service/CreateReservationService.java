package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.*;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link CreateReservationUseCase} by reading resources under shared locks, saving the
 * reservation, and allocating its requested slots in one transaction. Creation time comes from
 * {@link Clock} after every resource is protected.
 *
 * <p>Resources are protected in room, seat, then slot order, and each protected read returns the
 * row it locks: a deleted room or seat is rejected with {@link NoSuchElementException}, a seat
 * belonging to another room with {@link IllegalArgumentException}, and a missing slot row with
 * {@link NoSuchElementException}. Slots are locked in ascending identifier order, keeping the lock
 * order identical across transactions.
 *
 * <p>Only a complete allocation succeeds. If no slots or only some requested slots are allocated,
 * an exception rolls back the reservation header and any allocations inserted by this request.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateReservationService implements CreateReservationUseCase {
    private final ReservationSavePort reservationSavePort;
    private final ReserveSchedulePort reserveSchedulePort;
    private final RoomLockFindPort roomLockFindPort;
    private final SeatLockFindPort seatLockFindPort;
    private final ScheduleLockFindPort scheduleLockFindPort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Propagates protected read and persistence failures. An incomplete allocation raises an
     * exception so the transaction rolls back the header and all new slots.
     */
    @Override
    @Transactional
    public CreateReservationResult create(CreateReservationCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithSharedLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var seat =
                seatLockFindPort
                        .findByIdWithSharedLock(command.seatId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("seat does not exists."));

        if (!seat.getRoomId().equals(room.getId()))
            throw new IllegalArgumentException("seat must be belong to room.");

        scheduleLockFindPort
                .findAllByIdsWithSharedLock(command.scheduleIds())
                .orElseThrow(() -> new NoSuchElementException("schedule does not found."));

        var now = clock.instant();
        var timezone = clock.getZone();

        var reservationId = ReservationId.of(uuidGenerator.generate());
        var reservation =
                Reservation.of(
                        reservationId,
                        room.getId(),
                        seat.getId(),
                        command.reservationDate(),
                        timezone.getId(),
                        now);

        reservationSavePort.save(reservation);

        var reserveStatus = reserveSchedulePort.reserve(reservation.getId(), command.scheduleIds());

        if (reserveStatus == ReserveScheduleStatus.NONE_RESERVED
                || reserveStatus == ReserveScheduleStatus.PARTIAL_RESERVED) {
            throw new IllegalStateException("failed to reserve schedules.");
        }
        return CreateReservationResult.from(reservation, command.scheduleIds());
    }
}
