package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.RoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements room persistence ports using Spring Data JPA within the caller's transaction. */
@Component
@RequiredArgsConstructor
public class RoomJpaAdapter implements RoomFindPort, RoomSavePort, RoomLockFindPort {
    private final RoomRepository repository;

    /** {@inheritDoc} */
    @Override
    public Optional<Room> findById(RoomId roomId) {
        return repository.findById(roomId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Room> findNotDeletedById(RoomId roomId) {
        return repository.findById(roomId).filter(room -> !room.isDeleted());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Room> findDeletedById(RoomId roomId) {
        return repository.findById(roomId).filter(Room::isDeleted);
    }

    /** {@inheritDoc} */
    @Override
    public void save(Room room) {
        repository.save(room);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lock and the read are requested by one query, so the returned room is the row this
     * transaction protects. A missing row is reported as an empty result.
     */
    @Override
    public Optional<Room> findLockedById(RoomId roomId) {
        return repository.findByIdWithSharedLock(roomId);
    }
}
