package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import com.lilamaris.lilserve.reservation.application.port.in.FindScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class FindScheduleService implements FindScheduleUseCase {
    private final RoomFindPort roomFindPort;
    private final ScheduleFindPort scheduleFindPort;
    private final BlackoutScheduleFindPort blackoutScheduleFindPort;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public FindScheduleResult find(FindScheduleQuery query) {
        var room =
                roomFindPort
                        .findNotDeletedById(query.roomId())
                        .orElseThrow(() -> new NoSuchElementException("room does not exists."));

        var schedules = scheduleFindPort.findNotDeletedByRoomId(query.roomId());

        var blackouts =
                blackoutScheduleFindPort
                        .findNotDeletedByRoomIdAt(query.roomId(), query.now())
                        .stream()
                        .collect(Collectors.groupingBy(BlackoutSchedule::getScheduleId));

        var daySchedules = new ArrayList<DaySchedule>();

        for (var schedule : schedules) {
            var daySchedule =
                    DaySchedule.from(schedule, blackouts.getOrDefault(schedule.getId(), List.of()));

            daySchedules.add(daySchedule);
        }

        return FindScheduleResult.of(room.getId(), daySchedules);
    }
}
