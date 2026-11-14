package com.lilamaris.lilserve.identity.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The identifier of a user, embedded as its primary key. Equality is based on the UUID value. */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private UserId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a user identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the user identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
