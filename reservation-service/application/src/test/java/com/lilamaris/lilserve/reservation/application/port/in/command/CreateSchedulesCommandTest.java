package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("복수 스케줄 생성 커맨드")
class CreateSchedulesCommandTest {
    private static final RoomId ROOM =
            RoomId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final LocalTimeRange MORNING =
            LocalTimeRange.of(LocalTime.of(10, 0), LocalTime.of(12, 0));
    private static final LocalTimeRange EVENING =
            LocalTimeRange.of(LocalTime.of(14, 0), LocalTime.of(16, 0));
    private static final DayOfWeek MONDAY = DayOfWeek.MONDAY;
    private static final DayOfWeek TUESDAY = DayOfWeek.TUESDAY;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("복수 스케줄 커맨드는 원본 맵을 변경해도 생성 시점의 요일을 유지한다")
    void copiesSchedulesAtCommandCreation(boolean useFactory) {
        var source = new LinkedHashMap<DayOfWeek, List<LocalTimeRange>>();
        source.put(MONDAY, List.of(MORNING));

        var command = batchCommand(source, useFactory);
        source.put(TUESDAY, List.of(EVENING));

        assertThat(command.schedules()).containsOnlyKeys(MONDAY);
        assertThat(command.schedules().get(MONDAY)).containsExactly(MORNING);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("복수 스케줄 커맨드는 원본 리스트를 변경해도 생성 시점의 시간 범위를 유지한다")
    void copiesRangesAtCommandCreation(boolean useFactory) {
        var ranges = new ArrayList<>(List.of(MORNING));
        var command = batchCommand(Map.of(MONDAY, ranges), useFactory);

        ranges.add(EVENING);

        assertThat(command.schedules().get(MONDAY)).containsExactly(MORNING);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("복수 스케줄 커맨드의 요일별 맵은 변경할 수 없다")
    void rejectsSchedulesMutation(boolean useFactory) {
        var command = batchCommand(Map.of(MONDAY, List.of(MORNING)), useFactory);

        assertThatThrownBy(() -> command.schedules().put(TUESDAY, List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("복수 스케줄 커맨드의 시간 범위 리스트는 변경할 수 없다")
    void rejectsRangesMutation(boolean useFactory) {
        var ranges = new ArrayList<>(List.of(MORNING));
        var command = batchCommand(Map.of(MONDAY, ranges), useFactory);

        assertThatThrownBy(() -> command.schedules().get(MONDAY).add(EVENING))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CreateSchedulesCommand batchCommand(
            Map<DayOfWeek, List<LocalTimeRange>> schedules, boolean useFactory) {
        return useFactory
                ? CreateSchedulesCommand.of(ROOM, schedules)
                : new CreateSchedulesCommand(ROOM, schedules);
    }
}
