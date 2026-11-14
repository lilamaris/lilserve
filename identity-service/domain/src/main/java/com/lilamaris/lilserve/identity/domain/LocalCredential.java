package com.lilamaris.lilserve.identity.domain;

import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Email and password credentials belonging to one user. The caller validates and normalizes the
 * email and hashes the password before passing them to this entity. This entity neither verifies
 * passwords nor decides whether its user may authenticate.
 */
@Entity
@Table(name = "local_credential")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalCredential {
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_PASSWORD_HASH_LENGTH = 255;

    @EmbeddedId
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_id", updatable = false, nullable = false))
    private UserId userId;

    @Column(name = "email", nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false, length = MAX_PASSWORD_HASH_LENGTH)
    private String passwordHash;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private LocalCredential(UserId userId, String email, String passwordHash, Instant createdAt) {
        this.userId = Objects.requireNonNull(userId);
        this.email = StringPrecondition.requireMaxLen(email, MAX_EMAIL_LENGTH);
        this.passwordHash =
                StringPrecondition.requireMaxLen(passwordHash, MAX_PASSWORD_HASH_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Creates unverified credentials with no update timestamp. The user identifier is the primary
     * key, allowing at most one local credential per user.
     *
     * @param userId the owning user identifier
     * @param email the validated, normalized login email
     * @param passwordHash the encoded password, never a raw password
     * @param createdAt the creation time
     * @return the created credentials
     * @throws IllegalValueConditionException if {@code email} or {@code passwordHash} is blank
     * @throws IllegalComparisonConditionException if either string exceeds its column length
     * @throws NullPointerException if any argument is null
     */
    public static LocalCredential of(
            UserId userId, String email, String passwordHash, Instant createdAt) {
        return new LocalCredential(userId, email, passwordHash, createdAt);
    }

    /**
     * Changes the login email and clears verification of the previous address. Passing the same
     * normalized email preserves its verification and still records the update time.
     *
     * @param email the validated, normalized login email
     * @param updatedAt the update time, not before creation or the last update
     * @throws IllegalValueConditionException if {@code email} is blank
     * @throws IllegalComparisonConditionException if the email exceeds its column length or the
     *     update time is before creation or the last update
     * @throws NullPointerException if {@code email} or {@code updatedAt} is null
     */
    public void updateEmail(String email, Instant updatedAt) {
        StringPrecondition.requireMaxLen(email, MAX_EMAIL_LENGTH);
        update(updatedAt);
        if (!this.email.equals(email)) this.emailVerifiedAt = null;
        this.email = email;
    }

    /**
     * Replaces the encoded password and records the update time. Email verification is preserved.
     *
     * @param passwordHash the new encoded password, never a raw password
     * @param updatedAt the update time, not before creation or the last update
     * @throws IllegalValueConditionException if {@code passwordHash} is blank
     * @throws IllegalComparisonConditionException if the hash exceeds its column length or the
     *     update time is before creation or the last update
     * @throws NullPointerException if {@code passwordHash} or {@code updatedAt} is null
     */
    public void updatePasswordHash(String passwordHash, Instant updatedAt) {
        StringPrecondition.requireMaxLen(passwordHash, MAX_PASSWORD_HASH_LENGTH);
        update(updatedAt);
        this.passwordHash = passwordHash;
    }

    /**
     * Records verification of the current email after the caller validates proof for that address.
     *
     * @param email the validated, normalized email bound to the verification proof
     * @param verifiedAt the verification time, not before creation or the last update
     * @throws IllegalStateException if the email is already verified
     * @throws IllegalArgumentException if {@code email} does not match the current login email
     * @throws IllegalComparisonConditionException if the time is before creation or the last update
     * @throws NullPointerException if {@code email} or {@code verifiedAt} is null
     */
    public void verifyEmail(String email, Instant verifiedAt) {
        Objects.requireNonNull(email);
        if (!this.email.equals(email)) throw new IllegalArgumentException("Email does not match.");
        if (isEmailVerified()) throw new IllegalStateException("Email already verified.");
        update(verifiedAt);
        this.emailVerifiedAt = verifiedAt;
    }

    /**
     * Returns whether the current login email has been verified.
     *
     * @return true if {@code emailVerifiedAt} is set
     */
    public boolean isEmailVerified() {
        return this.emailVerifiedAt != null;
    }

    private void update(Instant updatedAt) {
        Objects.requireNonNull(updatedAt);
        TimePrecondition.requireAfterOrEqual(updatedAt, this.createdAt);
        if (this.updatedAt != null) TimePrecondition.requireAfterOrEqual(updatedAt, this.updatedAt);
        this.updatedAt = updatedAt;
    }
}
