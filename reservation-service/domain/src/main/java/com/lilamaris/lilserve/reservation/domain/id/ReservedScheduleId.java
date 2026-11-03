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
 * The identifier of a dated seat allocation, embedded as the primary key of the reserved_schedule
 * row. Two allocations are the same row exactly when their values are equal, and equality is the
 * value alone.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservedScheduleId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private ReservedScheduleId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates an allocation identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the allocation identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static ReservedScheduleId of(UUID value) {
        return new ReservedScheduleId(value);
    }
}
