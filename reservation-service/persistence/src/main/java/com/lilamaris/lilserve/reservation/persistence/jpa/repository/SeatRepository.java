package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, SeatId> {}
