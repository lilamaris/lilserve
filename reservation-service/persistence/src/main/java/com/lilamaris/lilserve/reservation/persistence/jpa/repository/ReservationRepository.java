package com.lilamaris.lilserve.reservation.persistence.jpa.repository;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides JPA persistence operations for reservation headers. */
public interface ReservationRepository extends JpaRepository<Reservation, ReservationId> {
    /**
     * Finds a non-cancelled header whose earliest active allocation starts after the given time.
     *
     * @param reservationId the reservation identifier
     * @param now the exclusive lower bound for the earliest active allocation's start
     * @return the matching header, or empty if no eligible reservation exists
     */
    @Query(
            """
            SELECT r
            FROM Reservation r
            WHERE r.id = :reservationId
                AND r.deletedAt IS NULL
                AND :now < (
                    SELECT MIN(rs.startsAt)
                    FROM ReservedSchedule rs
                    WHERE rs.reservationId = r.id
                        AND rs.deletedAt IS NULL
                )
            """)
    Optional<Reservation> findNotStartedById(
            @Param("reservationId") ReservationId reservationId, @Param("now") Instant now);
}
