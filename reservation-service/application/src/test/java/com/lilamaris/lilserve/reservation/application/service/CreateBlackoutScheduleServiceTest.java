package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("차단 스케줄 생성 유스케이스")
class CreateBlackoutScheduleServiceTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(60);
    private static final RoomId ROOM = RoomId.of(UUID.randomUUID());
    private static final ScheduleId SLOT = ScheduleId.of(UUID.randomUUID());
    private static final UUID ID = UUID.randomUUID();
    @Mock private RoomLockFindPort rooms;
    @Mock private ScheduleLockFindPort schedules;
    @Mock private BlackoutScheduleSavePort storage;
    private CreateBlackoutScheduleService service;

    @BeforeEach
    void setUp() {
        service =
                new CreateBlackoutScheduleService(
                        storage, rooms, schedules, () -> ID, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("방과 스케줄 및 요청 구간을 생성한 식별자와 현재 시각으로 저장한다")
    void savesRequestedBlackout() {
        givenRoom(false);
        givenSchedule(false);
        var command = command();
        service.create(command);
        var captured = ArgumentCaptor.forClass(BlackoutSchedule.class);
        verify(storage).save(captured.capture());
        var saved = captured.getValue();
        assertThat(saved.getId()).isEqualTo(BlackoutScheduleId.of(ID));
        assertThat(saved.getRoomId()).isEqualTo(ROOM);
        assertThat(saved.getScheduleId()).isEqualTo(SLOT);
        assertThat(saved.getReason()).isEqualTo(command.reason());
        assertThat(saved.getStartsAt()).isEqualTo(command.range().startsAt());
        assertThat(saved.getEndsAt()).isEqualTo(command.range().endsAt());
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("없는 방에는 차단을 생성하지 않는다")
    void rejectsMissingRoom() {
        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("room does not exists.");
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("삭제된 방에는 차단을 생성하지 않는다")
    void rejectsDeletedRoom() {
        givenRoom(true);
        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("room does not exists.");
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("없는 스케줄에는 차단을 생성하지 않는다")
    void rejectsMissingSchedule() {
        givenRoom(false);
        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("schedule does not exists.");
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("폐기된 스케줄에는 차단을 생성하지 않는다")
    void rejectsRetiredSchedule() {
        givenRoom(false);
        givenSchedule(true);
        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("schedule does not exists.");
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("저장 실패를 호출자에게 전파한다")
    void propagatesSaveFailure() {
        givenRoom(false);
        givenSchedule(false);
        var failure = new IllegalStateException("Storage failed");
        doThrow(failure).when(storage).save(any());
        assertThatThrownBy(() -> service.create(command())).isSameAs(failure);
    }

    private void givenRoom(boolean deleted) {
        var room = Room.of(ROOM, "ROOM-01", "hall", "", CREATED);
        if (deleted) room.delete(NOW);
        when(rooms.findByIdWithSharedLock(ROOM)).thenReturn(Optional.of(room));
    }

    private void givenSchedule(boolean deleted) {
        var schedule =
                Schedule.of(
                        SLOT,
                        ROOM,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        CREATED);
        if (deleted) schedule.delete(NOW);
        when(schedules.findByIdWithExclusiveLock(SLOT)).thenReturn(Optional.of(schedule));
    }

    private CreateBlackoutScheduleCommand command() {
        return CreateBlackoutScheduleCommand.of(
                ROOM,
                SLOT,
                InstantRange.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200)),
                "Maintenance");
    }
}
