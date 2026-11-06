package com.lilamaris.lilserve.reservation.domain;

import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The header of a reservation: one seat, one service date, and one timezone, grouping at least one
 * {@link ReservedSchedule allocation}. Cancellation applies to the whole header together with all
 * of its allocations; partial cancellation of individual allocations is not supported.
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
    @EmbeddedId private ReservationId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "room_id", nullable = false))
    private RoomId roomId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "seat_id", nullable = false))
    private SeatId seatId;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Reservation(
            ReservationId id,
            RoomId roomId,
            SeatId seatId,
            LocalDate reservationDate,
            String timeZone,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null.");
        this.roomId = Objects.requireNonNull(roomId, "roomId must not be null.");
        this.seatId = Objects.requireNonNull(seatId, "seatId must not be null.");
        this.reservationDate =
                Objects.requireNonNull(reservationDate, "reservationDate must not be null.");
        this.timeZone = ZoneId.of(timeZone).getId();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null.");
    }

    /**
     * Creates a reservation header for one seat on one service date.
     *
     * @param id the reservation identifier
     * @param roomId the room containing the seat
     * @param seatId the reserved seat
     * @param reservationDate the service date
     * @param timeZone the application timezone supplied by the writer, for example {@code
     *     Asia/Seoul}
     * @param createdAt the creation time
     * @return the reservation header
     * @throws NullPointerException if any argument is null
     * @throws java.time.DateTimeException if {@code timeZone} is not a valid timezone
     */
    public static Reservation of(
            ReservationId id,
            RoomId roomId,
            SeatId seatId,
            LocalDate reservationDate,
            String timeZone,
            Instant createdAt) {
        return new Reservation(id, roomId, seatId, reservationDate, timeZone, createdAt);
    }

    /**
     * Cancels the whole reservation. The allocations of this header must be cancelled in the same
     * transaction. The reservation workflow must also require at least one allocation.
     *
     * @param deletedAt the cancellation time, at or after {@code createdAt}
     * @throws NullPointerException if {@code deletedAt} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     deletedAt} is before {@code createdAt}
     * @throws IllegalStateException if the header is already cancelled
     */
    public void delete(Instant deletedAt) {
        validateNotDeleted();
        TimePrecondition.requireAfterOrEqual(deletedAt, createdAt);
        this.deletedAt = deletedAt;
    }

    /**
     * Checks whether the reservation has been cancelled.
     *
     * @return true if the reservation has a deletion time
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Requires that the reservation is still active.
     *
     * @throws IllegalStateException if the reservation has been cancelled
     */
    public void validateNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Deleted reservation cannot be used.");
        }
    }

    /**
     * Requires that the reservation has been cancelled.
     *
     * @throws IllegalStateException if the reservation is still active
     */
    public void validateDeleted() {
        if (!isDeleted()) {
            throw new IllegalStateException("Not deleted reservation cannot be used.");
        }
    }

    /**
     * Returns the timezone stored on the header as a {@link ZoneId}.
     *
     * @return the stored timezone
     * @throws java.time.DateTimeException if the stored timezone is not a valid timezone
     */
    public ZoneId zone() {
        return ZoneId.of(timeZone);
    }
}
