package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("방 스케줄 조회 유스케이스")
class ListScheduleServiceTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(60);
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    @Mock private RoomFindPort roomFindPort;
    @Mock private ScheduleFindPort scheduleFindPort;
    @Mock private BlackoutScheduleFindPort blackoutScheduleFindPort;
    private ListScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ListScheduleService(roomFindPort, scheduleFindPort, blackoutScheduleFindPort);
    }

    @Test
    @DisplayName("방의 모든 차단을 등록 스케줄과 무관하게 각 슬롯에 반환한다")
    void appliesRoomBlackoutsToEverySchedule() {
        givenRoom();
        var morning = schedule(10);
        var afternoon = schedule(14);
        var first = blackout(morning, "Maintenance");
        var second = blackout(morning, "Inspection");
        var other = blackout(afternoon, "Cleaning");
        when(scheduleFindPort.findNotDeletedByRoomId(ROOM_ID))
                .thenReturn(List.of(morning, afternoon));
        when(blackoutScheduleFindPort.findNotDeletedByRoomIdAt(ROOM_ID, NOW))
                .thenReturn(List.of(first, other, second));

        var result = service.list(ListScheduleQuery.of(ROOM_ID, NOW));

        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.schedules()).hasSize(2);
        var morningResult =
                result.schedules().stream()
                        .filter(slot -> slot.scheduleId().equals(morning.getId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(morningResult.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(morningResult.period())
                .isEqualTo(LocalTimeRange.of(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertThat(morningResult.blackouts())
                .extracting(summary -> summary.blackoutScheduleId())
                .containsExactlyInAnyOrder(first.getId(), other.getId(), second.getId());
        assertThat(morningResult.blackouts())
                .extracting(summary -> summary.reason())
                .containsExactlyInAnyOrder("Maintenance", "Cleaning", "Inspection");
        assertThat(morningResult.blackouts())
                .allSatisfy(
                        summary ->
                                assertThat(summary.validPeriod())
                                        .isEqualTo(InstantRange.of(CREATED, NOW.plusSeconds(60))));
        var afternoonResult =
                result.schedules().stream()
                        .filter(slot -> slot.scheduleId().equals(afternoon.getId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(afternoonResult.blackouts())
                .extracting(summary -> summary.blackoutScheduleId())
                .containsExactlyInAnyOrder(first.getId(), other.getId(), second.getId());
    }

    @Test
    @DisplayName("차단이 없는 스케줄도 빈 차단 목록과 함께 반환한다")
    void returnsScheduleWithoutBlackouts() {
        givenRoom();
        var schedule = schedule(10);
        when(scheduleFindPort.findNotDeletedByRoomId(ROOM_ID)).thenReturn(List.of(schedule));
        when(blackoutScheduleFindPort.findNotDeletedByRoomIdAt(ROOM_ID, NOW)).thenReturn(List.of());

        var result = service.list(ListScheduleQuery.of(ROOM_ID, NOW));

        assertThat(result.schedules())
                .singleElement()
                .satisfies(
                        slot -> {
                            assertThat(slot.scheduleId()).isEqualTo(schedule.getId());
                            assertThat(slot.blackouts()).isEmpty();
                        });
    }

    @Test
    @DisplayName("스케줄이 없는 방은 빈 스케줄 목록을 반환한다")
    void returnsEmptySchedulesForRoomWithoutSlots() {
        givenRoom();
        when(scheduleFindPort.findNotDeletedByRoomId(ROOM_ID)).thenReturn(List.of());
        when(blackoutScheduleFindPort.findNotDeletedByRoomIdAt(ROOM_ID, NOW)).thenReturn(List.of());

        var result = service.list(ListScheduleQuery.of(ROOM_ID, NOW));

        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.schedules()).isEmpty();
    }

    @Test
    @DisplayName("방이 없거나 삭제되어 조회되지 않으면 조회 실패를 알린다")
    void rejectsMissingRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(ListScheduleQuery.of(ROOM_ID, NOW)))
                .isInstanceOf(NoSuchElementException.class);
    }

    private void givenRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID))
                .thenReturn(Optional.of(Room.of(ROOM_ID, "ROOM-01", "hall", "", CREATED)));
    }

    private Schedule schedule(int start) {
        return Schedule.of(
                ScheduleId.of(UUID.randomUUID()),
                ROOM_ID,
                DayOfWeek.MONDAY,
                LocalTime.of(start, 0),
                LocalTime.of(start + 1, 0),
                CREATED);
    }

    private BlackoutSchedule blackout(Schedule schedule, String reason) {
        return BlackoutSchedule.of(
                BlackoutScheduleId.of(UUID.randomUUID()),
                ROOM_ID,
                schedule.getId(),
                reason,
                CREATED,
                NOW.plusSeconds(60),
                CREATED);
    }
}
