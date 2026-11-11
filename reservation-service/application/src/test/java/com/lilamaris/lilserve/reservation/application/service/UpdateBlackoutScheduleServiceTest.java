package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies blackout mutation results and rejection of missing or deleted resources after locked
 * reads.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("차단 스케줄 변경 및 삭제 유스케이스")
class UpdateBlackoutScheduleServiceTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(60);
    private static final RoomId ROOM = RoomId.of(UUID.randomUUID());
    private static final ScheduleId SLOT = ScheduleId.of(UUID.randomUUID());
    private static final BlackoutScheduleId ID = BlackoutScheduleId.of(UUID.randomUUID());
    @Mock private BlackoutScheduleFindPort finder;
    @Mock private BlackoutScheduleLockFindPort locks;
    @Mock private ScheduleLockFindPort schedules;
    @Mock private BlackoutScheduleSavePort storage;
    private UpdateBlackoutScheduleService service;

    @BeforeEach
    void setUp() {
        service =
                new UpdateBlackoutScheduleService(
                        finder, locks, schedules, storage, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("사유만 변경하고 식별자와 기존 구간을 보존한다")
    void updatesReasonWithoutChangingPeriod() {
        var blackout = givenBlackout();
        service.updateReason(UpdateBlackoutScheduleReasonCommand.of(ID, "Inspection"));
        assertThat(blackout.getReason()).isEqualTo("Inspection");
        assertThat(blackout.getStartsAt()).isEqualTo(CREATED.plusSeconds(3600));
        assertThat(blackout.getEndsAt()).isEqualTo(CREATED.plusSeconds(7200));
        assertUpdated(blackout);
    }

    @Test
    @DisplayName("기간을 변경하고 기존 사유와 소속을 보존한다")
    void updatesPeriodWithoutChangingReason() {
        givenSchedule();
        var blackout = givenBlackout();
        service.updateRange(UpdateBlackoutSchedulePeriodCommand.of(ID, range()));
        assertThat(blackout.getStartsAt()).isEqualTo(range().startsAt());
        assertThat(blackout.getEndsAt()).isEqualTo(range().endsAt());
        assertThat(blackout.getReason()).isEqualTo("Maintenance");
        assertUpdated(blackout);
    }

    @Test
    @DisplayName("삭제 시 삭제 시각과 수정 시각을 함께 저장한다")
    void savesDeletionTime() {
        givenSchedule();
        var blackout = givenBlackout();
        service.delete(DeleteBlackoutScheduleCommand.of(ID));
        assertThat(blackout.getDeletedAt()).isEqualTo(NOW);
        assertUpdated(blackout);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("락 조회에서 사라진 차단은 변경하거나 저장하지 않는다")
    void rejectsBlackoutMissingAfterLock(Change change) {
        if (change != Change.REASON) givenSchedule();
        assertThatThrownBy(() -> change(change)).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("락 획득 전에 삭제된 차단의 상태를 보존하고 요청을 거절한다")
    void rejectsDeletedBlackoutAfterLock(Change change) {
        if (change != Change.REASON) givenSchedule();
        var blackout = givenBlackout();
        var deletedAt = CREATED.plusSeconds(1);
        blackout.delete(deletedAt);
        assertThatThrownBy(() -> change(change)).isInstanceOf(NoSuchElementException.class);
        assertThat(blackout.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(blackout.getUpdatedAt()).isEqualTo(deletedAt);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"PERIOD", "DELETE"})
    @DisplayName("차단의 부모 식별자가 없으면 요청을 거절한다")
    void rejectsMissingParentIdentifier(Change change) {
        assertThatThrownBy(() -> change(change)).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"PERIOD", "DELETE"})
    @DisplayName("부모 스케줄이 없으면 차단을 저장하지 않는다")
    void rejectsMissingSchedule(Change change) {
        when(finder.findScheduleIdById(ID)).thenReturn(Optional.of(SLOT));
        assertThatThrownBy(() -> change(change)).isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("변경 저장 실패를 호출자에게 전파한다")
    void propagatesSaveFailure(Change change) {
        if (change != Change.REASON) givenSchedule();
        givenBlackout();
        var failure = new IllegalStateException("Storage failed");
        doThrow(failure).when(storage).save(any());
        assertThatThrownBy(() -> change(change)).isSameAs(failure);
    }

    private BlackoutSchedule givenBlackout() {
        var blackout =
                BlackoutSchedule.of(
                        ID,
                        ROOM,
                        SLOT,
                        "Maintenance",
                        CREATED.plusSeconds(3600),
                        CREATED.plusSeconds(7200),
                        CREATED);
        when(locks.findByIdWithExclusiveLock(ID)).thenReturn(Optional.of(blackout));
        return blackout;
    }

    private void givenSchedule() {
        when(finder.findScheduleIdById(ID)).thenReturn(Optional.of(SLOT));
        when(schedules.findByIdWithExclusiveLock(SLOT))
                .thenReturn(
                        Optional.of(
                                Schedule.of(
                                        SLOT,
                                        ROOM,
                                        DayOfWeek.MONDAY,
                                        LocalTime.of(10, 0),
                                        LocalTime.of(11, 0),
                                        CREATED)));
    }

    private void assertUpdated(BlackoutSchedule blackout) {
        assertThat(blackout.getId()).isEqualTo(ID);
        assertThat(blackout.getRoomId()).isEqualTo(ROOM);
        assertThat(blackout.getScheduleId()).isEqualTo(SLOT);
        assertThat(blackout.getCreatedAt()).isEqualTo(CREATED);
        assertThat(blackout.getUpdatedAt()).isEqualTo(NOW);
        verify(storage).save(blackout);
    }

    private InstantRange range() {
        return InstantRange.of(NOW.plusSeconds(10800), NOW.plusSeconds(14400));
    }

    private void change(Change change) {
        switch (change) {
            case REASON ->
                    service.updateReason(UpdateBlackoutScheduleReasonCommand.of(ID, "Inspection"));
            case PERIOD -> service.updateRange(UpdateBlackoutSchedulePeriodCommand.of(ID, range()));
            case DELETE -> service.delete(DeleteBlackoutScheduleCommand.of(ID));
        }
    }

    private enum Change {
        REASON,
        PERIOD,
        DELETE
    }
}
