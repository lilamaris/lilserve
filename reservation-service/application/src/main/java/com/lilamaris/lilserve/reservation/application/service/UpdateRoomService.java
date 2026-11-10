package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements room update, deletion, and restoration use cases. Each operation loads a room in the
 * required deletion state, applies the change using {@link Clock}, and saves it through {@link
 * RoomSavePort} in one transaction.
 *
 * <p>Changes to the same room are serialized in lock acquisition order. After waiting, each request
 * validates the locked state: modifications and deletion require an active room, while restoration
 * requires a deleted room. An incompatible state is rejected with {@link NoSuchElementException},
 * preserving the preceding change and its timestamps. Unchecked failures roll back all changes in
 * the transaction.
 */
@Service
@Validated
@RequiredArgsConstructor
public class UpdateRoomService
        implements UpdateRoomMetadataUseCase,
                UpdateRoomCodeUseCase,
                DeleteRoomUseCase,
                RestoreRoomUseCase {
    private final RoomLockFindPort roomLockFindPort;
    private final RoomSavePort roomSavePort;
    private final Clock clock;

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a room, requires it to be active, then changes its code using the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateRoomResult updateCode(@NotNull @Valid UpdateRoomCodeCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithExclusiveLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        room.updateCode(command.code(), now);

        roomSavePort.save(room);

        return UpdateRoomResult.from(room);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a room, requires it to be active, then updates its name and description
     * with the same clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateRoomResult updateMetadata(@NotNull @Valid UpdateRoomMetadataCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithExclusiveLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        room.updateMetadata(command.name(), command.description(), now);

        roomSavePort.save(room);

        return UpdateRoomResult.from(room);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a room, requires it to be active, then marks it as deleted at the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateRoomResult delete(DeleteRoomCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithExclusiveLock(command.roomId())
                        .filter(locked -> !locked.isDeleted())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        room.delete(now);

        roomSavePort.save(room);

        return UpdateRoomResult.from(room);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Exclusively locks a room, requires it to be deleted, then clears its deletion at the
     * current clock time, and saves it.
     */
    @Override
    @Transactional
    public UpdateRoomResult restore(RestoreRoomCommand command) {
        var room =
                roomLockFindPort
                        .findByIdWithExclusiveLock(command.roomId())
                        .filter(Room::isDeleted)
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var now = clock.instant();

        room.restore(now);

        roomSavePort.save(room);

        return UpdateRoomResult.from(room);
    }
}
