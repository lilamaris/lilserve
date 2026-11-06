package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import org.springframework.data.jpa.repository.JpaRepository;

/** Provides JPA persistence operations for recurring slots. */
public interface ScheduleRepository extends JpaRepository<Schedule, ScheduleId> {}
