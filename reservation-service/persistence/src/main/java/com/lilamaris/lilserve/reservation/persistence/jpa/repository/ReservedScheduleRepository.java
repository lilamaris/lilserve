package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.ReservedSchedule;
import com.lilamaris.lilserve.reservation.domain.id.ReservedScheduleId;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservedScheduleRepository
        extends JpaRepository<ReservedSchedule, ReservedScheduleId> {
    /**
     * Flushes pending changes, then inserts allocations from the header and active slots matching
     * its room and service weekday. PostgreSQL 18 generates allocation IDs with {@code uuidv7()}.
     * Unique and exclusion conflicts skip individual rows; other constraint failures propagate.
     *
     * <p>The returned count is provisional until the caller commits. If it is smaller than the
     * requested slot count, the caller must roll back the header and every inserted allocation.
     *
     * @param reservationId the active header identifier
     * @param schedules non-empty selected slot identifiers, without duplicates or null elements
     * @return the number of allocations inserted by this statement
     */
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO reserved_schedule (
                id,
                reservation_id,
                room_id,
                seat_id,
                schedule_id,
                start_at,
                end_at,
                reservation_date,
                time_zone,
                created_at
            )
            SELECT
                uuidv7(),
                r.id,
                r.room_id,
                r.seat_id,
                s.id,
                s.start_at,
                s.end_at,
                r.reservation_date,
                r.time_zone,
                r.created_at
            FROM reservation r
            JOIN schedule s
                ON s.room_id = r.room_id
            WHERE r.id = :reservationId
                AND r.deleted_at IS NULL
                AND s.id IN (:scheduleIds)
                AND s.deleted_at IS NULL
                AND s.day_of_week =
                    EXTRACT(ISODOW FROM r.reservation_date)
            ON CONFLICT DO NOTHING
            """,
            nativeQuery = true)
    int insertFromSchedules(
            @Param("reservationId") UUID reservationId,
            @Param("scheduleIds") Collection<UUID> schedules);

    /**
     * Flushes pending changes and marks all active allocations of the reservation as cancelled.
     * Already cancelled rows are left unchanged. The caller owns the transaction; this method does
     * not commit independently.
     *
     * @param reservationId the reservation header identifier
     * @param deletedAt the cancellation time, at or after the allocations' creation times
     * @return the number of allocations changed by this statement
     */
    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE reserved_schedule rs
                    SET deleted_at = :deletedAt
                    WHERE rs.reservation_id = :reservationId
                        AND rs.deleted_at IS NULL
                    """,
            nativeQuery = true)
    int markAsDeletedByReservationId(
            @Param("reservationId") UUID reservationId, @Param("deletedAt") Instant deletedAt);
}
