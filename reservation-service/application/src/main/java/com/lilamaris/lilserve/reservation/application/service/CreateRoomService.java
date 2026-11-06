package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link CreateRoomUseCase} by generating an identifier and saving a room in one
 * transaction. Creation time comes from {@link Clock}.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateRoomService implements CreateRoomUseCase {
    private final RoomSavePort roomSavePort;
    private final IdGenerator<UUID> uuidGenerator;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>The room is saved only after the domain constructor has accepted the command values.
     */
    @Override
    @Transactional
    public CreateRoomResult create(@NotNull @Valid CreateRoomCommand command) {
        var now = clock.instant();
        var roomId = RoomId.of(uuidGenerator.generate());
        var room = Room.of(roomId, command.code(), command.name(), command.description(), now);

        roomSavePort.save(room);

        return CreateRoomResult.from(room);
    }
}
