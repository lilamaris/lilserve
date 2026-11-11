package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link CreateSeatUseCase} by locking the parent room, generating an identifier, and
 * saving a seat in one transaction. Creation time comes from {@link Clock}.
 *
 * <p>A shared row lock is acquired before checking that the parent room is active and retained
 * until the transaction ends. Competing room deletion must wait for creation to commit or roll
 * back.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateSeatService implements CreateSeatUseCase {
    private final SeatSavePort seatSavePort;
    private final RoomLockFindPort roomLockFindPort;
    private final IdGenerator<UUID> uuidIdGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Loads the parent room under a shared lock and rejects a missing or deleted room before
     * creating the seat. The lock protects the active state until transaction completion. A
     * preceding room deletion that commits causes rejection; if it rolls back, creation can
     * proceed. A runtime failure rolls back creation, including changes already flushed to the
     * database.
     */
    @Override
    @Transactional
    public CreateSeatResult create(CreateSeatCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithSharedLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();
        var seatId = SeatId.of(uuidIdGenerator.generate());
        var seat = Seat.of(seatId, room.getId(), command.code(), now);

        seatSavePort.save(seat);

        return CreateSeatResult.from(seat);
    }
}
