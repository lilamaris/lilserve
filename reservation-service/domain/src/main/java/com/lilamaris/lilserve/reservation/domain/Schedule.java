package com.lilamaris.lilserve.reservation.domain;

import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.reservation.domain.converter.IsoDayOfWeekConverter;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A recurring room slot: one ISO weekday with one local time range. Slot definitions are
 * append-only, so a slot is retired by setting {@code deletedAt} and replaced by a new row. The
 * times of an existing slot are never edited in place, which keeps the times that past allocations
 * were created against. A retired slot only stops being offered; it does not cancel allocations.
 */
@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {
    @EmbeddedId private ScheduleId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "room_id", nullable = false))
    private RoomId roomId;

    @Convert(converter = IsoDayOfWeekConverter.class)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_at", nullable = false)
    private LocalTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalTime endAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Schedule(
            ScheduleId id,
            RoomId roomId,
            DayOfWeek dayOfWeek,
            LocalTime startAt,
            LocalTime endAt,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null.");
        this.roomId = Objects.requireNonNull(roomId, "roomId must not be null.");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null.");
        this.startAt = Objects.requireNonNull(startAt, "startAt must not be null.");
        this.endAt = Objects.requireNonNull(endAt, "endAt must not be null.");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null.");
        TimePrecondition.requireAfter(endAt, startAt);
    }

    /**
     * Creates a recurring slot for a room.
     *
     * @param id the slot identifier
     * @param roomId the room that offers the slot
     * @param dayOfWeek the ISO weekday the slot repeats on
     * @param startAt the local start time
     * @param endAt the local end time, strictly after {@code startAt}
     * @param createdAt the creation time
     * @return the slot
     * @throws NullPointerException if any argument is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     endAt} is not after {@code startAt}
     */
    public static Schedule of(
            UUID id,
            UUID roomId,
            DayOfWeek dayOfWeek,
            LocalTime startAt,
            LocalTime endAt,
            Instant createdAt) {
        return new Schedule(
                ScheduleId.of(id), RoomId.of(roomId), dayOfWeek, startAt, endAt, createdAt);
    }

    /**
     * Retires the slot so it is no longer offered for new allocations. Existing allocations keep
     * their reference to this slot.
     *
     * @param deletedAt the retirement time, at or after {@code createdAt}
     * @throws NullPointerException if {@code deletedAt} is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if {@code
     *     deletedAt} is before {@code createdAt}
     * @throws IllegalStateException if the slot is already retired
     */
    public void delete(Instant deletedAt) {
        validateNotDeleted();
        TimePrecondition.requireAfterOrEqual(deletedAt, createdAt);
        this.deletedAt = deletedAt;
    }

    /**
     * Checks whether the slot has been retired.
     *
     * @return true if the slot has a deletion time
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Requires that the slot is still offered, for example before a new allocation is created.
     *
     * @throws IllegalStateException if the slot has been retired
     */
    public void validateNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Deleted schedule cannot be used.");
        }
    }

    /**
     * Requires that the slot has been retired.
     *
     * @throws IllegalStateException if the slot is still offered
     */
    public void validateDeleted() {
        if (!isDeleted()) {
            throw new IllegalStateException("Not deleted schedule cannot be used.");
        }
    }
}
