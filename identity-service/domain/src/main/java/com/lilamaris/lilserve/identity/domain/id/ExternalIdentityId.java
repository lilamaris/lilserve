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
 * The identifier of an external identity, embedded as its primary key. Equality is based on the
 * UUID value.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalIdentityId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private ExternalIdentityId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates an external identity identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the external identity identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static ExternalIdentityId of(UUID value) {
        return new ExternalIdentityId(value);
    }
}
