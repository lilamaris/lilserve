package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.RoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomJpaAdapter implements RoomFindPort, RoomSavePort {
    private final RoomRepository repository;

    @Override
    public Optional<Room> findById(RoomId roomId) {
        return repository.findById(roomId);
    }

    @Override
    public Optional<Room> findNotDeletedById(RoomId roomId) {
        return repository.findById(roomId).filter(room -> !room.isDeleted());
    }

    @Override
    public Optional<Room> findDeletedById(RoomId roomId) {
        return repository.findById(roomId).filter(Room::isDeleted);
    }

    @Override
    public void save(Room room) {
        repository.save(room);
    }
}
