package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("스케줄 생성 커맨드")
class CreateScheduleCommandTest {
    private static final RoomId ROOM =
            RoomId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final LocalTime START_AT = LocalTime.of(10, 0);
    private static final LocalTime END_AT = LocalTime.of(12, 0);
    private static final DayOfWeek MONDAY = DayOfWeek.MONDAY;

    @ParameterizedTest
    @MethodSource("rangesEndingAtOrBeforeStart")
    @DisplayName("종료 시각이 시작 시각 이후가 아니면 단건 커맨드를 만들 수 없다")
    void rejectsCommandEndingAtOrBeforeStart(LocalTime startAt, LocalTime endAt) {
        assertThatThrownBy(() -> CreateScheduleCommand.of(ROOM, startAt, endAt, MONDAY))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @ParameterizedTest
    @MethodSource("rangesWithNullTime")
    @DisplayName("시작 또는 종료 시각이 없으면 단건 커맨드를 만들 수 없다")
    void rejectsCommandWithNullTime(LocalTime startAt, LocalTime endAt) {
        assertThatThrownBy(() -> CreateScheduleCommand.of(ROOM, startAt, endAt, MONDAY))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> rangesEndingAtOrBeforeStart() {
        return Stream.of(arguments(START_AT, START_AT), arguments(END_AT, START_AT));
    }

    private static Stream<Arguments> rangesWithNullTime() {
        return Stream.of(arguments(null, END_AT), arguments(START_AT, null));
    }
}
