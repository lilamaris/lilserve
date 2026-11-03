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
 * The identifier of a schedule, embedded as the primary key of the schedule row. Two schedules are
 * the same slot exactly when their values are equal, and equality is the value alone.
 */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private ScheduleId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a schedule identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the schedule identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static ScheduleId of(UUID value) {
        return new ScheduleId(value);
    }
}
