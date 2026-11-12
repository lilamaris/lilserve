package com.lilamaris.lilserve.reservation.application.port.in.result;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.util.List;

public record FindScheduleResult(RoomId roomId, List<DaySchedule> schedules) {
    /**
     * Copies the slot list to preserve the result independently of the supplied list.
     *
     * @param roomId the room identifier
     * @param schedules the room slots, in unspecified order
     */
    public FindScheduleResult {
        schedules = List.copyOf(schedules);
    }

    /**
     * Creates a room schedule result.
     *
     * @param roomId the room identifier
     * @param daySchedules the room slots and their active blackouts
     * @return the result with an immutable slot list
     */
    public static FindScheduleResult of(RoomId roomId, List<DaySchedule> daySchedules) {
        return new FindScheduleResult(roomId, daySchedules);
    }
}
