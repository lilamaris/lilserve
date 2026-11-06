package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides JPA persistence operations for rooms. */
public interface RoomRepository extends JpaRepository<Room, RoomId> {}
