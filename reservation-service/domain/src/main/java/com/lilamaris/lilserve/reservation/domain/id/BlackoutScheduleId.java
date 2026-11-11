package com.lilamaris.lilserve.reservation.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The identifier of a slot blackout. Equality is based on the UUID value. */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlackoutScheduleId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private BlackoutScheduleId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a blackout identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the blackout identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static BlackoutScheduleId of(UUID value) {
        return new BlackoutScheduleId(value);
    }
}
