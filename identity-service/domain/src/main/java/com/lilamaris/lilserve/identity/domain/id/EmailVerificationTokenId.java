package com.lilamaris.lilserve.identity.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The identifier of an email verification token, embedded as its primary key. Equality is based on
 * the UUID value.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationTokenId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private EmailVerificationTokenId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates an email verification token identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the email verification token identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static EmailVerificationTokenId of(UUID value) {
        return new EmailVerificationTokenId(value);
    }
}
