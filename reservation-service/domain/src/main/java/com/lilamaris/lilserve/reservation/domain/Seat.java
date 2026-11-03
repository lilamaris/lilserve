package com.lilamaris.lilserve.reservation.domain;

import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
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

/** A seat in a room. Code changes, deletion, and restoration record the latest update time. */
@Entity
@Table(name = "seat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {
    public static final int MAX_CODE_LENGTH = 20;

    @EmbeddedId private SeatId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "room_id", nullable = false))
    private RoomId roomId;

    @Column(name = "code", nullable = false, length = MAX_CODE_LENGTH)
    private String code;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Seat(SeatId id, RoomId roomId, String code, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.roomId = Objects.requireNonNull(roomId);
        this.code = StringPrecondition.requireMaxLen(code, MAX_CODE_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Creates a seat with no update and no deletion timestamp.
     *
     * @param id the seat identifier
     * @param roomId the room the seat belongs to
     * @param code the seat code
     * @param createdAt the creation time
     * @return the created seat
     * @throws IllegalValueConditionException if {@code code} is blank
     * @throws IllegalComparisonConditionException if {@code code} is above its column length
     * @throws NullPointerException if {@code id}, {@code roomId}, {@code code}, or {@code
     *     createdAt} is null
     */
    public static Seat of(SeatId id, RoomId roomId, String code, Instant createdAt) {
        return new Seat(id, roomId, code, createdAt);
    }

    /**
     * Changes the seat code and records {@code updatedAt} as the new update time.
     *
     * @param code the new code
     * @param updatedAt the update time, not before the last update time
     * @throws IllegalStateException if the seat is already deleted
     * @throws IllegalValueConditionException if {@code code} is blank
     * @throws IllegalComparisonConditionException if {@code code} is above its column length, or
     *     {@code updatedAt} is before {@code createdAt} or before the last update time
     * @throws NullPointerException if {@code code} or {@code updatedAt} is null
     */
    public void updateCode(String code, Instant updatedAt) {
        validateNotDeleted();
        StringPrecondition.requireMaxLen(code, MAX_CODE_LENGTH);
        update(updatedAt);
        this.code = code;
    }

    /**
     * Returns whether the seat has been deleted.
     *
     * @return true if {@code deletedAt} is set
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Requires that the seat is not deleted.
     *
     * @throws IllegalStateException if the seat is already deleted
     */
    public void validateNotDeleted() {
        if (isDeleted()) throw new IllegalStateException("Already deleted.");
    }

    /**
     * Requires that the seat is deleted.
     *
     * @throws IllegalStateException if the seat is not deleted
     */
    public void validateDeleted() {
        if (!isDeleted()) throw new IllegalStateException("Already not deleted.");
    }

    /**
     * Deletes the seat once and moves {@code updatedAt} to {@code deletedAt}.
     *
     * @param deletedAt the deletion time, not before the last update time
     * @throws IllegalStateException if the seat is already deleted
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
     * @throws IllegalStateException if the seat is not deleted
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
