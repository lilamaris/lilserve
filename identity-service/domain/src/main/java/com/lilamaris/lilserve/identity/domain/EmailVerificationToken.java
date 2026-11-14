package com.lilamaris.lilserve.identity.domain;

import com.lilamaris.lilserve.identity.domain.id.EmailVerificationTokenId;
import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.condition.StringPrecondition;
import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
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
 * A single-use proof request for a user's email address. Only the token hash is stored. The caller
 * generates and hashes the secret, verifies submitted proof, and atomically consumes this token
 * together with verification of the matching local credential.
 */
@Entity
@Table(name = "email_verification_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken {
    public static final int MAX_TOKEN_HASH_LENGTH = 255;

    @EmbeddedId private EmailVerificationTokenId id;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_id", updatable = false, nullable = false))
    private UserId userId;

    @Column(
            name = "email",
            updatable = false,
            nullable = false,
            length = LocalCredential.MAX_EMAIL_LENGTH)
    private String email;

    @Column(
            name = "token_hash",
            updatable = false,
            nullable = false,
            length = MAX_TOKEN_HASH_LENGTH)
    private String tokenHash;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false, nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private EmailVerificationToken(
            EmailVerificationTokenId id,
            UserId userId,
            String email,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.email = StringPrecondition.requireMaxLen(email, LocalCredential.MAX_EMAIL_LENGTH);
        this.tokenHash = StringPrecondition.requireMaxLen(tokenHash, MAX_TOKEN_HASH_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = TimePrecondition.requireAfter(expiresAt, createdAt);
    }

    /**
     * Creates an unused token bound to the supplied user and email. Neither consumption nor
     * revocation is recorded initially. Email validation and normalization belong to the caller.
     *
     * @param id the token identifier
     * @param userId the user whose email is being verified
     * @param email the validated, normalized email at issuance
     * @param tokenHash the hash of the generated secret, never the raw token
     * @param createdAt the issuance time
     * @param expiresAt the expiry time, strictly after issuance
     * @return the created verification token
     * @throws IllegalValueConditionException if {@code email} or {@code tokenHash} is blank
     * @throws IllegalComparisonConditionException if a string exceeds its column length or expiry
     *     is not strictly after issuance
     * @throws NullPointerException if any argument is null
     */
    public static EmailVerificationToken of(
            EmailVerificationTokenId id,
            UserId userId,
            String email,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt) {
        return new EmailVerificationToken(id, userId, email, tokenHash, createdAt, expiresAt);
    }

    /**
     * Consumes an unused, unrevoked token within its validity interval. The caller must verify
     * proof and the current credential's user and email before completing the transaction.
     *
     * @param consumedAt the consumption time, at or after issuance and strictly before expiry
     * @throws IllegalStateException if the token has been consumed or revoked
     * @throws IllegalComparisonConditionException if the time is before issuance or at or after
     *     expiry
     * @throws NullPointerException if {@code consumedAt} is null
     */
    public void consume(Instant consumedAt) {
        validatePending();
        TimePrecondition.requireAfterOrEqual(consumedAt, this.createdAt);
        TimePrecondition.requireBefore(consumedAt, this.expiresAt);
        this.consumedAt = consumedAt;
    }

    /**
     * Revokes an unused token once. Expired tokens may also be revoked; consumed tokens retain
     * their consumption state. There is no restoration operation.
     *
     * @param revokedAt the revocation time, not before issuance
     * @throws IllegalStateException if the token has been consumed or revoked
     * @throws IllegalComparisonConditionException if the time is before issuance
     * @throws NullPointerException if {@code revokedAt} is null
     */
    public void revoke(Instant revokedAt) {
        validatePending();
        TimePrecondition.requireAfterOrEqual(revokedAt, this.createdAt);
        this.revokedAt = revokedAt;
    }

    /**
     * Returns whether the expiry time has been reached, independently of consumption or revocation.
     *
     * @param now the time to evaluate
     * @return true if {@code now} is at or after expiry
     * @throws NullPointerException if {@code now} is null
     */
    public boolean isExpired(Instant now) {
        return !Objects.requireNonNull(now).isBefore(this.expiresAt);
    }

    private void validatePending() {
        if (this.consumedAt != null) throw new IllegalStateException("Already consumed.");
        if (this.revokedAt != null) throw new IllegalStateException("Already revoked.");
    }
}
