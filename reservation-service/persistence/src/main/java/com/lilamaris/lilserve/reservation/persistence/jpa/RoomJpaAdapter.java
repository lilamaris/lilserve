package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.RoomRepository;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public Optional<Room> findByIdWithSharedLock(RoomId roomId) {
        return repository.findByIdWithSharedLock(roomId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Room> findByIdWithExclusiveLock(RoomId roomId) {
        return repository.findByIdWithExclusiveLock(roomId);
    }

    /** {@inheritDoc} */
    @Override
    public OffsetPage<Room> findNotDeletedByFilters(
            @Nullable String name, @Nullable String code, OffsetPageRequest pageRequest) {
        var pageable = PageRequest.of(pageRequest.page(), pageRequest.size());
        var page =
                repository.findActiveByFilters(
                        name == null ? "" : name, code == null ? "" : code, pageable);
        return OffsetPage.of(
                page.getContent(), pageRequest.page(), pageRequest.size(), page.getTotalElements());
    }
}
