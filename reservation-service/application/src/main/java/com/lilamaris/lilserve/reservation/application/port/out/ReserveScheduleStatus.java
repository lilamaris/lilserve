package com.lilamaris.lilserve.reservation.application.port.out;

/** The outcome of allocating all requested slots to a reservation. */
public enum ReserveScheduleStatus {
    ALL_SCHEDULE_SUCCESSFULLY_RESERVED,
    FAILED
}
