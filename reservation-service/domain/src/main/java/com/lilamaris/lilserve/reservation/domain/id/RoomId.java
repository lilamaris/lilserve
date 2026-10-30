package com.lilamaris.lilserve.reservation.domain.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomId {
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID value;

    private RoomId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a room identifier from the UUID value.
     *
     * @param value the identifier value
     * @return the room identifier
     * @throws NullPointerException if {@code value} is null
     */
    public static RoomId of(UUID value) {
        return new RoomId(value);
    }
}
