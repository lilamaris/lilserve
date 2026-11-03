package com.lilamaris.lilserve.reservation.application.model.id;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * Generates time-ordered UUIDs, so new rows append to the primary key index instead of scattering
 * it.
 */
public class UuidGenerator implements IdGenerator<UUID> {

    /**
     * {@inheritDoc}
     *
     * @return a time-ordered UUID
     */
    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
