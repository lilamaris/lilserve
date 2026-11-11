package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlackoutScheduleRepository
        extends JpaRepository<BlackoutSchedule, BlackoutScheduleId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT bs
            FROM BlackoutSchedule bs
            WHERE bs.id = :blackoutScheduleId""")
    Optional<BlackoutSchedule> findByIdWithExclusiveLock(
            @Param("blackoutScheduleId") BlackoutScheduleId blackoutScheduleId);

    @Query(
            """
            SELECT bs.scheduleId
            FROM BlackoutSchedule bs
            WHERE bs.id = :blackoutScheduleId""")
    Optional<ScheduleId> findScheduleIdById(
            @Param("blackoutScheduleId") BlackoutScheduleId blackoutScheduleId);
}
