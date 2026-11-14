package com.lilamaris.lilserve.identity.domain;

import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A user independent of its login credentials. Changes record the latest update time; deletion
 * preserves the account status so restoration does not lift a suspension.
 */
@Entity
@Table(name = "identity_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;

    @EmbeddedId private UserId id;

    @Column(name = "display_name", nullable = false, length = MAX_DISPLAY_NAME_LENGTH)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private User(UserId id, String displayName, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.displayName = StringPrecondition.requireMaxLen(displayName, MAX_DISPLAY_NAME_LENGTH);
        this.status = UserStatus.ACTIVE;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Creates an active user with no update and no deletion timestamp.
     *
     * @param id the user identifier
     * @param displayName the display name
     * @param createdAt the creation time
     * @return the created user
     * @throws IllegalValueConditionException if {@code displayName} is blank
     * @throws IllegalComparisonConditionException if {@code displayName} is above its column length
     * @throws NullPointerException if {@code id}, {@code displayName}, or {@code createdAt} is null
     */
    public static User of(UserId id, String displayName, Instant createdAt) {
        return new User(id, displayName, createdAt);
    }

    /**
     * Changes the display name and records the update time. Suspended users can be renamed.
     *
     * @param displayName the new display name
     * @param updatedAt the update time, not before creation or the last update
     * @throws IllegalStateException if the user is deleted
     * @throws IllegalValueConditionException if {@code displayName} is blank
     * @throws IllegalComparisonConditionException if the name exceeds its column length or the
     *     update time is before creation or the last update
     * @throws NullPointerException if {@code displayName} or {@code updatedAt} is null
     */
    public void updateDisplayName(String displayName, Instant updatedAt) {
        validateNotDeleted();
        StringPrecondition.requireMaxLen(displayName, MAX_DISPLAY_NAME_LENGTH);
        update(updatedAt);
        this.displayName = displayName;
    }

    /**
     * Suspends an active user and records the update time.
     *
     * @param suspendedAt the suspension time, not before creation or the last update
     * @throws IllegalStateException if the user is deleted or already suspended
     * @throws IllegalComparisonConditionException if the time is before creation or the last update
     * @throws NullPointerException if {@code suspendedAt} is null
     */
    public void suspend(Instant suspendedAt) {
        validateNotDeleted();
        if (this.status == UserStatus.SUSPENDED)
            throw new IllegalStateException("Already suspended.");
        update(suspendedAt);
        this.status = UserStatus.SUSPENDED;
    }

    /**
     * Lifts a suspension and records the update time.
     *
     * @param resumedAt the resumption time, not before creation or the last update
     * @throws IllegalStateException if the user is deleted or already active
     * @throws IllegalComparisonConditionException if the time is before creation or the last update
     * @throws NullPointerException if {@code resumedAt} is null
     */
    public void resume(Instant resumedAt) {
        validateNotDeleted();
        if (this.status == UserStatus.ACTIVE) throw new IllegalStateException("Already active.");
        update(resumedAt);
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Returns whether the user is active and not deleted.
     *
     * @return true if the account status and deletion state allow authentication
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isDeleted();
    }

    /**
     * Returns whether the user has been deleted.
     *
     * @return true if {@code deletedAt} is set
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Requires that the user is not deleted.
     *
     * @throws IllegalStateException if the user is deleted
     */
    public void validateNotDeleted() {
        if (isDeleted()) throw new IllegalStateException("Already deleted.");
    }

    /**
     * Requires that the user is deleted.
     *
     * @throws IllegalStateException if the user is not deleted
     */
    public void validateDeleted() {
        if (!isDeleted()) throw new IllegalStateException("Already not deleted.");
    }

    /**
     * Deletes the user once and moves the update time to the deletion time. Account status is kept.
     *
     * @param deletedAt the deletion time, not before creation or the last update
     * @throws IllegalStateException if the user is already deleted
     * @throws IllegalComparisonConditionException if the time is before creation or the last update
     * @throws NullPointerException if {@code deletedAt} is null
     */
    public void delete(Instant deletedAt) {
        validateNotDeleted();
        update(deletedAt);
        this.deletedAt = deletedAt;
    }

    /**
     * Clears the deletion and records the restoration time. A suspended user remains suspended.
     *
     * @param restoredAt the restoration time, not before the last update
     * @throws IllegalStateException if the user is not deleted
     * @throws IllegalComparisonConditionException if the time is before creation or the last update
     * @throws NullPointerException if {@code restoredAt} is null
     */
    public void restore(Instant restoredAt) {
        validateDeleted();
        update(restoredAt);
        this.deletedAt = null;
    }

    private void update(Instant updatedAt) {
        Objects.requireNonNull(updatedAt);
        TimePrecondition.requireAfterOrEqual(updatedAt, this.createdAt);
        if (this.updatedAt != null) TimePrecondition.requireAfterOrEqual(updatedAt, this.updatedAt);
        this.updatedAt = updatedAt;
    }
}
