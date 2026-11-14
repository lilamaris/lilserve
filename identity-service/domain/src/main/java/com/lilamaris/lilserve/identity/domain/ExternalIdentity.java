package com.lilamaris.lilserve.identity.domain;

import com.lilamaris.lilserve.identity.domain.id.ExternalIdentityId;
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
 * A verified external account linked to a user. Issuer and subject identify the external account;
 * provider selects its server-configured integration. No email, password, or provider tokens are
 * stored here. An unlinked association cannot be restored without a new authentication flow.
 */
@Entity
@Table(name = "external_identity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalIdentity {
    public static final int MAX_PROVIDER_LENGTH = 100;
    public static final int MAX_ISSUER_LENGTH = 2048;
    public static final int MAX_SUBJECT_LENGTH = 255;

    @EmbeddedId private ExternalIdentityId id;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_id", updatable = false, nullable = false))
    private UserId userId;

    @Column(name = "provider", updatable = false, nullable = false, length = MAX_PROVIDER_LENGTH)
    private String provider;

    @Column(name = "issuer", updatable = false, nullable = false, length = MAX_ISSUER_LENGTH)
    private String issuer;

    @Column(name = "subject", updatable = false, nullable = false, length = MAX_SUBJECT_LENGTH)
    private String subject;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "unlinked_at")
    private Instant unlinkedAt;

    private ExternalIdentity(
            ExternalIdentityId id,
            UserId userId,
            String provider,
            String issuer,
            String subject,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.provider = StringPrecondition.requireMaxLen(provider, MAX_PROVIDER_LENGTH);
        this.issuer = StringPrecondition.requireMaxLen(issuer, MAX_ISSUER_LENGTH);
        this.subject = StringPrecondition.requireMaxLen(subject, MAX_SUBJECT_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * Links an externally authenticated account to a user. The caller verifies external proof and
     * prevents duplicate active associations; matching emails do not establish ownership.
     *
     * @param id the association identifier
     * @param userId the owning user identifier
     * @param provider the trusted integration identifier
     * @param issuer the validated OIDC issuer, or a trusted configured namespace for OAuth2-only
     *     providers
     * @param subject the validated, stable external user identifier, preserved exactly
     * @param createdAt the link creation time
     * @return the linked external identity
     * @throws IllegalValueConditionException if any string is blank
     * @throws IllegalComparisonConditionException if any string exceeds its column length
     * @throws NullPointerException if any argument is null
     */
    public static ExternalIdentity of(
            ExternalIdentityId id,
            UserId userId,
            String provider,
            String issuer,
            String subject,
            Instant createdAt) {
        return new ExternalIdentity(id, userId, provider, issuer, subject, createdAt);
    }

    /**
     * Unlinks this external account once. The caller ensures that the user retains a usable login
     * method and applies any required token revocation policy.
     *
     * @param unlinkedAt the unlink time, not before creation
     * @throws IllegalStateException if the association is already unlinked
     * @throws IllegalComparisonConditionException if the unlink time is before creation
     * @throws NullPointerException if {@code unlinkedAt} is null
     */
    public void unlink(Instant unlinkedAt) {
        if (!isLinked()) throw new IllegalStateException("Already unlinked.");
        Objects.requireNonNull(unlinkedAt);
        TimePrecondition.requireAfterOrEqual(unlinkedAt, this.createdAt);
        this.unlinkedAt = unlinkedAt;
    }

    /**
     * Returns whether this external account remains linked. User status is checked separately.
     *
     * @return true if {@code unlinkedAt} is not set
     */
    public boolean isLinked() {
        return this.unlinkedAt == null;
    }
}
