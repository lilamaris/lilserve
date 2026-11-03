package com.lilamaris.lilserve.reservation.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The identifier of a reservation header, embedded as the primary key of the reservation row. Two
 * reservations are the same header exactly when their values are equal, and equality is the value
 * alone.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private ReservationId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a reservation identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the reservation identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static ReservationId of(UUID value) {
        return new ReservationId(value);
    }
}
