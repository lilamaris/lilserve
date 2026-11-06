package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
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
 * Implements {@link CreateSeatUseCase} by loading the parent room, generating an identifier, and
 * saving a seat in one transaction. Creation time comes from {@link Clock}.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateSeatService implements CreateSeatUseCase {
    private final SeatSavePort seatSavePort;
    private final RoomFindPort roomFindPort;
    private final IdGenerator<UUID> uuidIdGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>The parent room is loaded through the non-deleted lookup before the seat is created and
     * saved.
     */
    @Override
    @Transactional
    public CreateSeatResult create(CreateSeatCommand command) {
        var room =
                roomFindPort
                        .findNotDeletedById(command.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();
        var seatId = SeatId.of(uuidIdGenerator.generate());
        var seat = Seat.of(seatId, room.getId(), command.code(), now);

        seatSavePort.save(seat);

        return CreateSeatResult.from(seat);
    }
}
