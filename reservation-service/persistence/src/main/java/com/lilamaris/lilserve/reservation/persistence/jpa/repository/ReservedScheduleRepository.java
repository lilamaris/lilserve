package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.ReservedSchedule;
import com.lilamaris.lilserve.reservation.domain.id.ReservedScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.row.ReservedScheduleRow;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence operations for dated slot allocations. */
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
     * Marks all active allocations of the reservation as cancelled and returns both the total and
     * updated allocation counts. Already cancelled rows retain their original cancellation times
     * and contribute only to the total count.
     *
     * <p>A PostgreSQL data-modifying CTE counts updated rows through {@code RETURNING} in the same
     * statement that counts all allocations. The caller owns the transaction, must flush pending
     * header changes before invoking this method, and must roll back an incomplete release. This
     * method does not commit independently.
     *
     * @param reservationId the reservation header identifier
     * @param deletedAt the cancellation time, at or after the allocations' creation times
     * @return the total allocation count and the number changed by this statement; both counts are
     *     zero if the reservation has no allocations
     */
    @Query(
            value =
                    """
                    WITH released AS (
                        UPDATE reserved_schedule rs
                        SET deleted_at = :deletedAt
                        WHERE rs.reservation_id = :reservationId
                            AND rs.deleted_at IS NULL
                        RETURNING id
                    )
                    SELECT
                        (
                            SELECT COUNT(*)
                            FROM reserved_schedule
                            WHERE reservation_id = :reservationId
                        ) AS total_count,
                        (
                            SELECT COUNT(*)
                            FROM released
                        ) AS updated_count
                    """,
            nativeQuery = true)
    ReservedScheduleRow.ReleasedSchedule markAsDeletedByReservationId(
            @Param("reservationId") UUID reservationId, @Param("deletedAt") Instant deletedAt);
}
