package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import com.lilamaris.lilserve.reservation.application.port.in.ListScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.ListScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link ListScheduleUseCase} by reading the room, its slots, and the blackouts active
 * at the requested usage instant, then attaching the same room restrictions to every slot.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ListScheduleService implements ListScheduleUseCase {
    private final RoomFindPort roomFindPort;
    private final ScheduleFindPort scheduleFindPort;
    private final BlackoutScheduleFindPort blackoutScheduleFindPort;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ListScheduleResult list(ListScheduleQuery query) {
        var room =
                roomFindPort
                        .findNotDeletedById(query.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var schedules = scheduleFindPort.findNotDeletedByRoomId(query.roomId());

        var blackouts =
                blackoutScheduleFindPort.findNotDeletedByRoomIdAt(query.roomId(), query.now());

        var daySchedules = new ArrayList<DaySchedule>();

        for (var schedule : schedules) {
            var daySchedule = DaySchedule.from(schedule, blackouts);

            daySchedules.add(daySchedule);
        }

        return ListScheduleResult.of(room.getId(), daySchedules);
    }
}
