package com.lilamaris.lilserve.reservation.domain;

import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A reservable room. Every change moves {@code updatedAt} forward, and deletion is recorded once in
 * {@code deletedAt}.
 */
@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {
    public static final int MAX_CODE_LENGTH = 20;
    public static final int MAX_NAME_LENGTH = 300;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    @EmbeddedId private RoomId id;

    @Column(name = "code", nullable = false, length = MAX_CODE_LENGTH)
    private String code;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "description", nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Room(RoomId id, String code, String name, String description, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.code = StringPrecondition.requireMaxLen(code, MAX_CODE_LENGTH);
        this.name = StringPrecondition.requireMaxLen(name, MAX_NAME_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.description =
                StringPrecondition.requireNonNullMaxLen(description, MAX_DESCRIPTION_LENGTH);
    }

    /**
     * Creates a room with no update and no deletion timestamp.
     *
     * @param id the room identifier
     * @param code the room code
     * @param name the room name
     * @param description the room description, blank allowed
     * @param createdAt the creation time
     * @return the created room
     * @throws IllegalValueConditionException if {@code code} or {@code name} is blank
     * @throws IllegalComparisonConditionException if {@code code}, {@code name}, or {@code
     *     description} is above its column length
     * @throws NullPointerException if {@code id}, {@code code}, {@code name}, {@code description},
     *     or {@code createdAt} is null
     */
    public static Room of(
            UUID id, String code, String name, String description, Instant createdAt) {
        var roomId = RoomId.of(id);
        return new Room(roomId, code, name, description, createdAt);
    }

    /**
     * Renames the room and records {@code updatedAt} as the new update time.
     *
     * @param name the new name
     * @param updatedAt the update time, not before the last update time
     * @throws IllegalStateException if the room is already deleted
     * @throws IllegalValueConditionException if {@code name} is blank
     * @throws IllegalComparisonConditionException if {@code name} is above its column length, or
     *     {@code updatedAt} is before {@code createdAt} or before the last update time
     * @throws NullPointerException if {@code name} or {@code updatedAt} is null
     */
    public void updateName(String name, Instant updatedAt) {
        validateNotDeleted();
        StringPrecondition.requireMaxLen(name, MAX_NAME_LENGTH);
        update(updatedAt);
        this.name = name;
    }

    /**
     * Changes the description and records {@code updatedAt} as the new update time. Blank
     * descriptions are accepted.
     *
     * @param description the new description, not null
     * @param updatedAt the update time, not before the last update time
     * @throws IllegalStateException if the room is already deleted
     * @throws IllegalComparisonConditionException if {@code description} is above its column
     *     length, or {@code updatedAt} is before {@code createdAt} or before the last update time
     * @throws NullPointerException if {@code description} or {@code updatedAt} is null
     */
    public void updateDescription(String description, Instant updatedAt) {
        validateNotDeleted();
        var validated =
                StringPrecondition.requireNonNullMaxLen(description, MAX_DESCRIPTION_LENGTH);
        update(updatedAt);
        this.description = validated;
    }

    /**
     * Changes the name and description together after validating both values and the update time.
     *
     * @param name the new name
     * @param description the new description, blank allowed
     * @param updatedAt the update time, not before creation or the last update time
     * @throws IllegalStateException if the room is already deleted
     * @throws IllegalValueConditionException if {@code name} is blank
     * @throws IllegalComparisonConditionException if either value exceeds its column length or
     *     {@code updatedAt} is before creation or the last update time
     * @throws NullPointerException if {@code name}, {@code description}, or {@code updatedAt} is
     *     null
     */
    public void updateMetadata(String name, String description, Instant updatedAt) {
        validateNotDeleted();
        StringPrecondition.requireMaxLen(name, MAX_NAME_LENGTH);
        StringPrecondition.requireNonNullMaxLen(description, MAX_DESCRIPTION_LENGTH);
        update(updatedAt);
        this.name = name;
        this.description = description;
    }

    /**
     * Changes the room code and records {@code updatedAt} as the new update time.
     *
     * @param code the new code
     * @param updatedAt the update time, not before the last update time
     * @throws IllegalStateException if the room is already deleted
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
     * Returns whether the room has been deleted.
     *
     * @return true if {@code deletedAt} is set
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Requires that the room is not deleted.
     *
     * @throws IllegalStateException if the room is already deleted
     */
    public void validateNotDeleted() {
        if (isDeleted()) throw new IllegalStateException("Already deleted.");
    }

    /**
     * Requires that the room is deleted.
     *
     * @throws IllegalStateException if the room is not deleted
     */
    public void validateDeleted() {
        if (!isDeleted()) throw new IllegalStateException("Already not deleted.");
    }

    /**
     * Deletes the room once and moves {@code updatedAt} to {@code deletedAt}.
     *
     * @param deletedAt the deletion time, not before the last update time
     * @throws IllegalStateException if the room is already deleted
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
     * @throws IllegalStateException if the room is not deleted
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
