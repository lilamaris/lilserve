package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("스케줄 생성 유스케이스")
class CreateScheduleServiceTest {
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final RoomId ROOM = RoomId.of(ROOM_ID);
    private static final UUID SCHEDULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SECOND_SCHEDULE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID THIRD_SCHEDULE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
    private static final LocalTime MORNING_START = LocalTime.of(10, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime EVENING_START = LocalTime.of(14, 0);
    private static final LocalTime EVENING_END = LocalTime.of(16, 0);
    private static final DayOfWeek MONDAY = DayOfWeek.MONDAY;
    private static final DayOfWeek TUESDAY = DayOfWeek.TUESDAY;
    private static final LocalTimeRange MORNING = range(MORNING_START, MORNING_END);
    private static final LocalTimeRange EVENING = range(EVENING_START, EVENING_END);

    @Mock private ScheduleSavePort scheduleSavePort;
    @Mock private RoomFindPort roomFindPort;

    private final List<Schedule> savedSchedules = new ArrayList<>();
    private CreateScheduleService service;

    @BeforeEach
    void setUp() {
        service =
                new CreateScheduleService(scheduleSavePort, roomFindPort, () -> SCHEDULE_ID, CLOCK);
    }

    @Test
    @DisplayName("부모 룸이 있으면 생성한 식별자와 현재 시각으로 스케줄을 저장한다")
    void savesScheduleWithGeneratedIdAndCurrentTime() {
        givenRoomFound();

        var result = service.create(command(MORNING_START, MORNING_END, MONDAY));

        var schedule = savedSchedule();
        assertThat(schedule.getId()).isEqualTo(ScheduleId.of(SCHEDULE_ID));
        assertThat(schedule.getRoomId()).isEqualTo(ROOM);
        assertThat(schedule.getDayOfWeek()).isEqualTo(MONDAY);
        assertThat(schedule.getStartAt()).isEqualTo(MORNING_START);
        assertThat(schedule.getEndAt()).isEqualTo(MORNING_END);
        assertThat(schedule.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(schedule.getDeletedAt()).isNull();
        assertThat(result)
                .isEqualTo(
                        new CreateScheduleResult(
                                ScheduleId.of(SCHEDULE_ID),
                                ROOM,
                                MONDAY,
                                MORNING_START,
                                MORNING_END,
                                CREATED_AT));
    }

    @Test
    @DisplayName("모든 요일의 모든 시간 범위를 각각 식별자 하나로 한 번에 저장한다")
    void savesOneSchedulePerDayAndRange() {
        givenRoomFound();
        givenBatchSave();
        service =
                new CreateScheduleService(
                        scheduleSavePort,
                        roomFindPort,
                        List.of(SCHEDULE_ID, SECOND_SCHEDULE_ID, THIRD_SCHEDULE_ID).iterator()
                                ::next,
                        CLOCK);

        var result = create(Map.of(MONDAY, List.of(MORNING), TUESDAY, List.of(EVENING, MORNING)));

        assertThat(savedSchedules).hasSize(3);
        assertThat(savedSchedules)
                .extracting(schedule -> schedule.getId().getValue())
                .containsExactlyInAnyOrder(SCHEDULE_ID, SECOND_SCHEDULE_ID, THIRD_SCHEDULE_ID);
        assertThat(savedSchedules)
                .extracting(Schedule::getDayOfWeek, Schedule::getStartAt, Schedule::getEndAt)
                .containsExactlyInAnyOrder(
                        tuple(MONDAY, MORNING_START, MORNING_END),
                        tuple(TUESDAY, EVENING_START, EVENING_END),
                        tuple(TUESDAY, MORNING_START, MORNING_END));
        assertThat(savedSchedules)
                .allSatisfy(
                        schedule -> {
                            assertThat(schedule.getRoomId()).isEqualTo(ROOM);
                            assertThat(schedule.getCreatedAt()).isEqualTo(CREATED_AT);
                            assertThat(schedule.getDeletedAt()).isNull();
                        });
        verify(scheduleSavePort, never()).save(any(Schedule.class));
        assertThat(result.schedules())
                .containsExactlyInAnyOrderElementsOf(
                        savedSchedules.stream()
                                .map(
                                        schedule ->
                                                new CreateScheduleResult(
                                                        schedule.getId(),
                                                        schedule.getRoomId(),
                                                        schedule.getDayOfWeek(),
                                                        schedule.getStartAt(),
                                                        schedule.getEndAt(),
                                                        schedule.getCreatedAt()))
                                .toList());
    }

    @Test
    @DisplayName("요일과 시간 범위가 없으면 저장할 스케줄이 없다")
    void savesNothingWhenCommandHasNoSchedules() {
        givenRoomFound();
        givenBatchSave();

        var result = create(Map.of());

        assertThat(savedSchedules).isEmpty();
        assertThat(result.schedules()).isEmpty();
        verify(scheduleSavePort, never()).save(any(Schedule.class));
    }

    @Test
    @DisplayName("부모 룸가 없으면 조회 실패 예외를 전파하고 스케줄를 저장하지 않는다")
    void rejectsMissingRoom() {
        givenRoomMissing();

        assertThatThrownBy(() -> service.create(command(MORNING_START, MORNING_END, MONDAY)))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("복수 스케줄도 부모 룸가 없으면 조회 실패 예외를 전파하고 저장하지 않는다")
    void rejectsMissingRoomForBatch() {
        givenRoomMissing();

        assertThatThrownBy(() -> create(Map.of(MONDAY, List.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("부모 룸 조회가 실패하면 해당 예외를 전파하고 스케줄를 저장하지 않는다")
    void propagatesRoomFindFailure() {
        var failure = new IllegalStateException("Room lookup failed");
        when(roomFindPort.findNotDeletedById(ROOM)).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(MORNING_START, MORNING_END, MONDAY)))
                .isSameAs(failure);

        verifyNoInteractions(scheduleSavePort);
    }

    @ParameterizedTest
    @MethodSource("commandsWithNullFields")
    @DisplayName("커맨드 필드가 없으면 예외를 전파하고 스케줄를 저장하지 않는다")
    void doesNotSaveWhenCommandFieldIsNull(CreateScheduleCommand command) {
        givenRoomFound();

        assertThatThrownBy(() -> service.create(command)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("식별자 생성이 실패하면 예외를 전파하고 스케줄를 저장하지 않는다")
    void propagatesIdGenerationFailureWithoutSaving() {
        givenRoomFound();
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                new CreateScheduleService(
                        scheduleSavePort,
                        roomFindPort,
                        (IdGenerator<UUID>)
                                () -> {
                                    throw failure;
                                },
                        CLOCK);

        assertThatThrownBy(() -> service.create(command(MORNING_START, MORNING_END, MONDAY)))
                .isSameAs(failure);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("복수 스케줄 생성 중 식별자 생성이 실패하면 이미 만든 스케줄도 저장하지 않는다")
    void propagatesIdGenerationFailureWithoutSavingAnySchedule() {
        givenRoomFound();
        var failure = new IllegalStateException("Identifier generation failed");
        var ids = List.of(SCHEDULE_ID, SECOND_SCHEDULE_ID).iterator();
        service =
                new CreateScheduleService(
                        scheduleSavePort,
                        roomFindPort,
                        () -> {
                            if (!ids.hasNext()) {
                                throw failure;
                            }
                            return ids.next();
                        },
                        CLOCK);

        assertThatThrownBy(() -> create(Map.of(MONDAY, List.of(MORNING, EVENING, MORNING))))
                .isSameAs(failure);

        verifyNoInteractions(scheduleSavePort);
    }

    @Test
    @DisplayName("스케줄 저장이 실패하면 저장 예외를 전파한다")
    void propagatesSaveFailure() {
        givenRoomFound();
        var failure = new IllegalStateException("Schedule storage failed");
        doThrow(failure).when(scheduleSavePort).save(any(Schedule.class));

        assertThatThrownBy(() -> service.create(command(MORNING_START, MORNING_END, MONDAY)))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("복수 스케줄 저장이 실패하면 저장 예외를 전파한다")
    void propagatesBatchSaveFailure() {
        givenRoomFound();
        var failure = new IllegalStateException("Schedule storage failed");
        doThrow(failure).when(scheduleSavePort).saveAll(any());

        assertThatThrownBy(() -> create(Map.of(MONDAY, List.of(MORNING)))).isSameAs(failure);
    }

    private void givenRoomFound() {
        var room = Room.of(ROOM, "ROOM-01", "main hall", "capacity 100", CREATED_AT);
        when(roomFindPort.findNotDeletedById(ROOM)).thenReturn(Optional.of(room));
    }

    private void givenRoomMissing() {
        when(roomFindPort.findNotDeletedById(ROOM)).thenReturn(Optional.empty());
    }

    private void givenBatchSave() {
        doAnswer(
                        invocation -> {
                            savedSchedules.addAll(invocation.getArgument(0));
                            return null;
                        })
                .when(scheduleSavePort)
                .saveAll(any());
    }

    private Schedule savedSchedule() {
        var captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleSavePort).save(captor.capture());
        return captor.getValue();
    }

    private CreateSchedulesResult create(Map<DayOfWeek, List<LocalTimeRange>> schedules) {
        return service.create(CreateSchedulesCommand.of(ROOM, schedules));
    }

    private static CreateScheduleCommand command(
            LocalTime startAt, LocalTime endAt, DayOfWeek dayOfWeek) {
        return CreateScheduleCommand.of(ROOM, startAt, endAt, dayOfWeek);
    }

    private static LocalTimeRange range(LocalTime startAt, LocalTime endAt) {
        return LocalTimeRange.of(startAt, endAt);
    }

    private static Stream<Arguments> commandsWithNullFields() {
        return Stream.of(
                arguments(new CreateScheduleCommand(ROOM, null, MONDAY)),
                arguments(new CreateScheduleCommand(ROOM, MORNING, null)));
    }
}
