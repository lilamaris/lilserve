package com.lilamaris.lilserve.reservation.web.dto;

/**
 * JSON bodies published in the OpenAPI document as response examples.
 *
 * <p>Every shape carries its nullable fields as {@code null}, so a consumer reading the document
 * sees the whole body it must handle. The response DTOs point their schema example at these
 * constants, so the document publishes one shape for a type in both places it appears.
 */
public final class OpenApiExamples {
    /** One room that has never been updated or deleted. */
    public static final String ROOM =
            "{\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"code\":\"ROOM-01\",\"name\":\"main hall\","
                    + "\"description\":\"capacity 100\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\","
                    + "\"updatedAt\":null,\"deletedAt\":null}";

    /** One seat of {@link #ROOM} that has never been updated or deleted. */
    public static final String SEAT =
            "{\"seatId\":\"00000000-0000-0000-0000-000000000002\","
                    + "\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"code\":\"A01\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\","
                    + "\"updatedAt\":null,\"deletedAt\":null}";

    /** One recurring slot of {@link #ROOM} on Monday morning. */
    public static final String SCHEDULE =
            "{\"scheduleId\":\"00000000-0000-0000-0000-000000000003\","
                    + "\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"dayOfWeek\":\"MONDAY\",\"startAt\":\"09:00:00\","
                    + "\"endAt\":\"11:00:00\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\"}";

    /** Two slots created by one batch, so the batch shape differs from one slot. */
    public static final String SCHEDULE_BATCH =
            "["
                    + "{\"scheduleId\":\"00000000-0000-0000-0000-000000000003\","
                    + "\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"dayOfWeek\":\"MONDAY\",\"startAt\":\"09:00:00\","
                    + "\"endAt\":\"11:00:00\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\"},"
                    + "{\"scheduleId\":\"00000000-0000-0000-0000-000000000004\","
                    + "\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"dayOfWeek\":\"TUESDAY\",\"startAt\":\"13:00:00\","
                    + "\"endAt\":\"15:00:00\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\"}]";

    /** One blackout of a recurring slot that has never been updated or deleted. */
    public static final String BLACKOUT =
            "{\"blackoutScheduleId\":\"00000000-0000-0000-0000-000000000005\","
                    + "\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"scheduleId\":\"00000000-0000-0000-0000-000000000003\","
                    + "\"reason\":\"facility maintenance\","
                    + "\"startsAt\":\"2026-01-02T00:00:00Z\","
                    + "\"endsAt\":\"2026-01-03T00:00:00Z\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\","
                    + "\"updatedAt\":null,\"deletedAt\":null}";

    /**
     * One active blackout as a slot list reports it, so it carries no creation, update, and
     * deletion timestamp.
     */
    public static final String SCHEDULE_BLACKOUT =
            "{\"blackoutScheduleId\":\"00000000-0000-0000-0000-000000000005\","
                    + "\"startsAt\":\"2026-01-02T00:00:00Z\","
                    + "\"endsAt\":\"2026-01-03T00:00:00Z\","
                    + "\"reason\":\"facility maintenance\"}";

    /** One slot of {@link #ROOM} with one blackout active at the requested instant. */
    public static final String SCHEDULE_SLOT =
            "{\"scheduleId\":\"00000000-0000-0000-0000-000000000003\","
                    + "\"dayOfWeek\":\"MONDAY\",\"startAt\":\"09:00:00\","
                    + "\"endAt\":\"11:00:00\","
                    + "\"blackouts\":["
                    + SCHEDULE_BLACKOUT
                    + "]}";

    /** The slots of {@link #ROOM} at one intake instant. */
    public static final String SCHEDULE_LIST =
            "{\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"schedules\":["
                    + SCHEDULE_SLOT
                    + "]}";

    /** First page of one room. */
    public static final String ROOM_PAGE =
            "{\"items\":[" + ROOM + "],\"page\":0,\"size\":20,\"totalElements\":1}";

    /** First page of one seat. */
    public static final String SEAT_PAGE =
            "{\"items\":[" + SEAT + "],\"page\":0,\"size\":20,\"totalElements\":1}";

    /** Available seat identifiers for one room and service date. */
    public static final String AVAILABLE_SEATS =
            "{\"roomId\":\"00000000-0000-0000-0000-000000000001\","
                    + "\"targetDate\":\"2026-11-16\","
                    + "\"availableSeatIds\":[\"00000000-0000-0000-0000-000000000002\"]}";

    private OpenApiExamples() {
        throw new UnsupportedOperationException("Constants holder.");
    }
}
