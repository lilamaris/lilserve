package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.util.List;

/**
 * The slots of one room and the reservation intake blackouts active at the requested instant.
 *
 * @param roomId the room the slots belong to
 * @param schedules the room slots, in unspecified order, empty when the room offers none
 */
public record ListScheduleResult(RoomId roomId, List<DaySchedule> schedules) {
    /**
     * Copies the slot list to preserve the result independently of the supplied list.
     *
     * @param roomId the room identifier
     * @param schedules the room slots, in unspecified order
     */
    public ListScheduleResult {
        schedules = List.copyOf(schedules);
    }

    /**
     * Creates a room slot list result.
     *
     * @param roomId the room identifier
     * @param daySchedules the room slots and their active blackouts
     * @return the result with an immutable slot list
     */
    public static ListScheduleResult of(RoomId roomId, List<DaySchedule> daySchedules) {
        return new ListScheduleResult(roomId, daySchedules);
    }
}
