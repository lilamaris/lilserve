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
 * The identifier of a seat, embedded as the primary key of the seat row. Two seats are the same
 * seat exactly when their values are equal, and equality is the value alone.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private SeatId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a seat identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the seat identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static SeatId of(UUID value) {
        return new SeatId(value);
    }
}
