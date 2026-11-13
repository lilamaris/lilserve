package com.lilamaris.lilserve.reservation.domain;

import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A temporary physical usage restriction for a room over an absolute time interval.
 *
 * <p>The half-open interval is compared with requested service usage, never the booking clock. The
 * restriction applies to every slot in the room, including replacements of retired slots. The
 * schedule association records the registration context only; it does not narrow the scope.
 * Existing reservations are preserved. The reason and interval may change while active; deletion
 * and restoration record the update time.
 */
@Entity
@Table(name = "blackout_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlackoutSchedule {
    /** Maximum stored reason length; the domain permits blank reasons. */
    public static final int MAX_REASON_LENGTH = 1000;

    @EmbeddedId private BlackoutScheduleId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "room_id", nullable = false))
    private RoomId roomId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "schedule_id", nullable = false))
    private ScheduleId scheduleId;

    @Column(name = "reason", nullable = false, length = MAX_REASON_LENGTH)
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private BlackoutSchedule(
            BlackoutScheduleId id,
            RoomId roomId,
            ScheduleId scheduleId,
            String reason,
            Instant startsAt,
            Instant endsAt,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.roomId = Objects.requireNonNull(roomId);
        this.scheduleId = Objects.requireNonNull(scheduleId);
        this.reason = StringPrecondition.requireNonNullMaxLen(reason, MAX_REASON_LENGTH);
        TimePrecondition.requireAfter(endsAt, startsAt);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Creates a blackout with no update or deletion timestamp. The database enforces that the slot
     * belongs to the room.
     *
     * @param id the blackout identifier
     * @param roomId the room that owns the slot
     * @param scheduleId the recurring slot to block
     * @param reason the blackout reason, blank allowed
     * @param startsAt the inclusive start instant
     * @param endsAt the exclusive end instant, strictly after {@code startsAt}
     * @param createdAt the creation time
     * @return the blackout
     * @throws NullPointerException if any argument is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     reason exceeds its column length or the end is not after the start
     */
    public static BlackoutSchedule of(
            BlackoutScheduleId id,
            RoomId roomId,
            ScheduleId scheduleId,
            String reason,
            Instant startsAt,
            Instant endsAt,
            Instant createdAt) {
        return new BlackoutSchedule(id, roomId, scheduleId, reason, startsAt, endsAt, createdAt);
    }

    /**
     * Changes the reason and records the update time.
     *
     * @param reason the new reason, blank allowed
     * @param updatedAt the update time, not before creation or the last update
     * @throws IllegalStateException if the blackout is deleted
     * @throws NullPointerException if either argument is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     reason exceeds its column length or the update time moves backwards
     */
    public void updateReason(String reason, Instant updatedAt) {
        validateNotDeleted();
        var validated = StringPrecondition.requireNonNullMaxLen(reason, MAX_REASON_LENGTH);
        update(updatedAt);
        this.reason = validated;
    }

    /**
     * Changes the room usage interval and records the update time.
     *
     * @param startsAt the inclusive start instant
     * @param endsAt the exclusive end instant, strictly after {@code startsAt}
     * @param updatedAt the update time, not before creation or the last update
     * @throws IllegalStateException if the blackout is deleted
     * @throws NullPointerException if any argument is null
     * @throws com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException if the
     *     end is not after the start or the update time moves backwards
     */
    public void updatePeriod(Instant startsAt, Instant endsAt, Instant updatedAt) {
        validateNotDeleted();
        TimePrecondition.requireAfter(endsAt, startsAt);
        update(updatedAt);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * Returns whether the blackout has been deleted.
     *
     * @return true if {@code deletedAt} is set
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Requires that the blackout is not deleted.
     *
     * @throws IllegalStateException if the blackout is already deleted
     */
    public void validateNotDeleted() {
        if (isDeleted()) throw new IllegalStateException("Already deleted.");
    }

    /**
     * Requires that the blackout is deleted.
     *
     * @throws IllegalStateException if the blackout is not deleted
     */
    public void validateDeleted() {
        if (!isDeleted()) throw new IllegalStateException("Already not deleted.");
    }

    /**
     * Deletes the blackout once and moves {@code updatedAt} to {@code deletedAt}.
     *
     * @param deletedAt the deletion time, not before the last update time
     * @throws IllegalStateException if the blackout is already deleted
     * @throws IllegalComparisonConditionException if {@code deletedAt} is before {@code createdAt}
     *     or before the last update time
     * @throws NullPointerException if {@code deletedAt} is null
     */
    public void delete(Instant deletedAt) {
        validateNotDeleted();
        validateDeletedAt(deletedAt);
        update(deletedAt);
        this.deletedAt = deletedAt;
    }

    /**
     * Clears the deletion and moves {@code updatedAt} to {@code restoredAt}.
     *
     * @param restoredAt the restore time, not before the last update time
     * @throws IllegalStateException if the blackout is not deleted
     * @throws IllegalComparisonConditionException if {@code restoredAt} is before {@code createdAt}
     *     or before the last update time
     * @throws NullPointerException if {@code restoredAt} is null
     */
    public void restore(Instant restoredAt) {
        validateDeleted();
        update(restoredAt);
        this.deletedAt = null;
    }

    private void update(Instant updatedAt) {
        validateUpdatedAt(updatedAt);
        this.updatedAt = updatedAt;
    }

    private void validateUpdatedAt(Instant updatedAt) {
        Objects.requireNonNull(updatedAt);
        TimePrecondition.requireAfterOrEqual(updatedAt, this.createdAt);
        if (this.updatedAt != null) TimePrecondition.requireAfterOrEqual(updatedAt, this.updatedAt);
    }

    private void validateDeletedAt(Instant deletedAt) {
        Objects.requireNonNull(deletedAt);
        var baseline = Objects.requireNonNullElse(this.updatedAt, this.createdAt);
        TimePrecondition.requireAfterOrEqual(deletedAt, baseline);
    }
}
