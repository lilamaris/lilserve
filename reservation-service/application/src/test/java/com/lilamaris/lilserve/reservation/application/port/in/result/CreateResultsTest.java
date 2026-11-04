package com.lilamaris.lilserve.reservation.application.port.in.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("예약과 스케줄 생성 결과의 불변 스냅샷")
class CreateResultsTest {
    @Test
    @DisplayName("일괄 생성 결과는 원본 목록의 변경에 영향받지 않고 반환 목록도 변경할 수 없다")
    void snapshotsCreatedSchedules() {
        var schedule =
                new CreateScheduleResult(
                        ScheduleId.of(UUID.randomUUID()),
                        RoomId.of(UUID.randomUUID()),
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        Instant.EPOCH);
        var source = new ArrayList<>(List.of(schedule));

        var result = new CreateSchedulesResult(source);
        source.clear();

        assertThat(result.schedules()).containsExactly(schedule);
        assertThatThrownBy(() -> result.schedules().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("예약 생성 결과는 원본 슬롯 집합의 변경에 영향받지 않고 반환 집합도 변경할 수 없다")
    void snapshotsReservedScheduleIds() {
        var scheduleId = ScheduleId.of(UUID.randomUUID());
        var source = new HashSet<>(Set.of(scheduleId));

        var result =
                new CreateReservationResult(
                        ReservationId.of(UUID.randomUUID()),
                        RoomId.of(UUID.randomUUID()),
                        SeatId.of(UUID.randomUUID()),
                        source,
                        LocalDate.of(2026, 11, 9),
                        "Asia/Seoul",
                        Instant.EPOCH);
        source.clear();

        assertThat(result.scheduleIds()).containsExactly(scheduleId);
        assertThatThrownBy(() -> result.scheduleIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
