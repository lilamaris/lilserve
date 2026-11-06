package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Room;

/** Persists rooms within the caller's transaction. */
public interface RoomSavePort {

    /**
     * Stores the room, inserting it when it is new.
     *
     * @param room the room to store, not null
     * @throws NullPointerException if {@code room} is null
     */
    void save(Room room);
}
