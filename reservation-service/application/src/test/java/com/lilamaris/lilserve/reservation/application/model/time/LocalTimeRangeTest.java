package com.lilamaris.lilserve.reservation.application.model.time;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import java.time.LocalTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("로컬 시간 범위")
class LocalTimeRangeTest {
    private static final LocalTime START_AT = LocalTime.of(10, 0);
    private static final LocalTime END_AT = LocalTime.of(12, 0);

    @ParameterizedTest
    @MethodSource("rangesEndingAtOrBeforeStart")
    @DisplayName("종료 시각이 시작 시각 이후여야 시간 범위를 만들 수 있다")
    void rejectsRangeEndingAtOrBeforeStart(LocalTime startAt, LocalTime endAt) {
        assertThatThrownBy(() -> LocalTimeRange.of(startAt, endAt))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @ParameterizedTest
    @MethodSource("rangesWithNullTime")
    @DisplayName("시간 범위는 시작과 종료 시각을 모두 요구한다")
    void rejectsRangeWithNullTime(LocalTime startAt, LocalTime endAt) {
        assertThatThrownBy(() -> LocalTimeRange.of(startAt, endAt))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> rangesEndingAtOrBeforeStart() {
        return Stream.of(arguments(START_AT, START_AT), arguments(END_AT, START_AT));
    }

    private static Stream<Arguments> rangesWithNullTime() {
        return Stream.of(arguments(null, END_AT), arguments(START_AT, null));
    }
}
