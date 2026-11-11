package com.lilamaris.lilserve.reservation.application.model.time;

import com.lilamaris.lilserve.kernel.condition.TimePrecondition;
import java.time.Instant;
import java.util.Objects;

public record InstantRange(Instant startsAt, Instant endsAt) {
    public InstantRange {
        Objects.requireNonNull(startsAt);
        TimePrecondition.requireAfter(endsAt, startsAt);
    }

    public static InstantRange of(Instant startsAt, Instant endsAt) {
        return new InstantRange(startsAt, endsAt);
    }
}
