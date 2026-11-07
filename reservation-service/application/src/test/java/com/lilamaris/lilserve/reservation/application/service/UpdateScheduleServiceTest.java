package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.out.RetireSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Schedule;
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
@DisplayName("스케줄 수정 및 삭제 유스케이스")
class UpdateScheduleServiceTest {
    private static final RoomId ROOM = RoomId.of(UUID.randomUUID());
    private static final ScheduleId ORIGINAL_ID = ScheduleId.of(UUID.randomUUID());
    private static final UUID REPLACEMENT_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant REPLACED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(REPLACED_AT, ZoneOffset.UTC);

    @Mock private ScheduleFindPort scheduleFindPort;
    @Mock private ScheduleSavePort scheduleSavePort;
    @Mock private RetireSchedulePort retireSchedulePort;

    private UpdateScheduleService service;

    @BeforeEach
    void setUp() {
        service =
                new UpdateScheduleService(
                        scheduleFindPort,
                        scheduleSavePort,
                        retireSchedulePort,
                        () -> REPLACEMENT_ID,
                        CLOCK);
    }

    @Test
    @DisplayName("원본의 시간 정의와 방을 보존하고 새 식별자로 교체한 스케줄을 반환한다")
    void retiresOriginalAndReturnsSavedReplacement() {
        var original = givenScheduleFound(CREATED_AT);

        var result = service.update(command());

        var captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleSavePort).save(captor.capture());
        var replacement = captor.getValue();
        assertThat(replacement.getId()).isEqualTo(ScheduleId.of(REPLACEMENT_ID));
        assertThat(replacement.getRoomId()).isEqualTo(ROOM);
        assertThat(replacement.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(replacement.getStartAt()).isEqualTo(LocalTime.of(14, 0));
        assertThat(replacement.getEndAt()).isEqualTo(LocalTime.of(16, 0));
        assertThat(replacement.getCreatedAt()).isEqualTo(REPLACED_AT);
        assertThat(replacement.isDeleted()).isFalse();
        assertThat(original.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(original.getStartAt()).isEqualTo(LocalTime.of(10, 0));
        assertThat(original.getEndAt()).isEqualTo(LocalTime.of(12, 0));
        assertThat(original.getDeletedAt()).isEqualTo(REPLACED_AT);
        verify(retireSchedulePort).retire(ORIGINAL_ID, REPLACED_AT);
        assertThat(result)
                .isEqualTo(
                        new UpdateScheduleResult(
                                ScheduleId.of(REPLACEMENT_ID),
                                ROOM,
                                DayOfWeek.TUESDAY,
                                LocalTime.of(14, 0),
                                LocalTime.of(16, 0),
                                REPLACED_AT));
    }

    @Test
    @DisplayName("활성 스케줄을 찾지 못하면 원본을 폐기하거나 대체 스케줄을 저장하지 않는다")
    void rejectsMissingActiveSchedule() {
        when(scheduleFindPort.findNotDeletedById(ORIGINAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(command()))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(retireSchedulePort, scheduleSavePort);
    }

    @Test
    @DisplayName("현재 시각이 원본 생성 시각보다 이르면 폐기와 신규 저장을 거부한다")
    void rejectsRetirementBeforeOriginalCreation() {
        givenScheduleFound(REPLACED_AT.plusSeconds(1));

        assertThatThrownBy(() -> service.update(command()))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(retireSchedulePort, scheduleSavePort);
    }

    @Test
    @DisplayName("식별자 생성 실패를 전파하고 폐기와 신규 저장을 수행하지 않는다")
    void propagatesIdGenerationFailure() {
        givenScheduleFound(CREATED_AT);
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                new UpdateScheduleService(
                        scheduleFindPort,
                        scheduleSavePort,
                        retireSchedulePort,
                        () -> {
                            throw failure;
                        },
                        CLOCK);

        assertThatThrownBy(() -> service.update(command())).isSameAs(failure);

        verifyNoInteractions(retireSchedulePort, scheduleSavePort);
    }

    @Test
    @DisplayName("폐기 저장 실패를 전파하고 대체 스케줄을 저장하지 않는다")
    void propagatesRetirementFailure() {
        givenScheduleFound(CREATED_AT);
        var failure = new IllegalStateException("Schedule retirement failed");
        doThrow(failure).when(retireSchedulePort).retire(ORIGINAL_ID, REPLACED_AT);

        assertThatThrownBy(() -> service.update(command())).isSameAs(failure);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("대체 스케줄 저장이 실패하면 결과 대신 저장 예외를 전파한다")
    void propagatesReplacementSaveFailure() {
        givenScheduleFound(CREATED_AT);
        var failure = new IllegalStateException("Schedule storage failed");
        doThrow(failure).when(scheduleSavePort).save(any(Schedule.class));

        assertThatThrownBy(() -> service.update(command())).isSameAs(failure);
    }

    @Test
    @DisplayName("삭제한 스케줄의 식별자와 저장된 삭제 시각을 반환한다")
    void returnsDeletedScheduleResult() {
        var schedule = givenScheduleFound(CREATED_AT);

        var result = service.delete(ORIGINAL_ID);

        assertThat(result).isEqualTo(new DeleteScheduleResult(ORIGINAL_ID, REPLACED_AT));
        assertThat(schedule.getDeletedAt()).isEqualTo(result.deletedAt());
        verify(scheduleSavePort).save(schedule);
    }

    private Schedule givenScheduleFound(Instant createdAt) {
        var schedule =
                Schedule.of(
                        ORIGINAL_ID,
                        ROOM,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        createdAt);
        when(scheduleFindPort.findNotDeletedById(ORIGINAL_ID)).thenReturn(Optional.of(schedule));
        return schedule;
    }

    private UpdateScheduleCommand command() {
        return UpdateScheduleCommand.of(
                ORIGINAL_ID,
                LocalTimeRange.of(LocalTime.of(14, 0), LocalTime.of(16, 0)),
                DayOfWeek.TUESDAY);
    }
}
