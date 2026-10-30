package com.lilamaris.lilserve.kernel.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilserve.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Number Precondition 테스트")
class NumberPreconditionTest {
    @Test
    @DisplayName("값만으로 음수 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenNegativeConditionMatches() {
        assertThat(NumberPrecondition.requireNegative(-1)).isEqualTo(-1);
    }

    @Test
    @DisplayName("값만으로 음수 조건을 검사하면 조건을 위반한 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesNegativeCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNegative(0))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be negative.");
        assertThatThrownBy(() -> NumberPrecondition.requireNegative(1))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be negative.");
    }

    @Test
    @DisplayName("값만으로 0 이상 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenNonNegativeConditionMatches() {
        assertThat(NumberPrecondition.requireNonNegative(0)).isEqualTo(0);
        assertThat(NumberPrecondition.requireNonNegative(1)).isEqualTo(1);
    }

    @Test
    @DisplayName("값만으로 0 이상 조건을 검사하면 음수 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesNonNegativeCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNonNegative(-1))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be non-negative.");
    }

    @Test
    @DisplayName("값만으로 양수 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenPositiveConditionMatches() {
        assertThat(NumberPrecondition.requirePositive(1)).isEqualTo(1);
    }

    @Test
    @DisplayName("값만으로 양수 조건을 검사하면 조건을 위반한 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesPositiveCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requirePositive(0))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
        assertThatThrownBy(() -> NumberPrecondition.requirePositive(-1))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
    }

    @Test
    @DisplayName("값만으로 0 이하 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenNonPositiveConditionMatches() {
        assertThat(NumberPrecondition.requireNonPositive(-1)).isEqualTo(-1);
        assertThat(NumberPrecondition.requireNonPositive(0)).isEqualTo(0);
    }

    @Test
    @DisplayName("값만으로 0 이하 조건을 검사하면 양수 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesNonPositiveCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNonPositive(1))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be non-positive.");
    }

    @Test
    @DisplayName("값만으로 부호 조건을 검사하면 예외가 조건과 검사를 받은 값을 담는다")
    void exceptionKeepsConditionAndSubjectForSignCondition() {
        Integer value = 0;

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> NumberPrecondition.requirePositive(value));

        assertThat(exception.condition()).isEqualTo(NumberCondition.POSITIVE);
        assertThat(exception.subject()).isEqualTo(value);
    }

    @Test
    @DisplayName("Integer Negative 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerNegativeValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNegative(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer NonNegative 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerNonNegativeValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNonNegative(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer Positive 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerPositiveValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requirePositive(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer NonPositive 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerNonPositiveValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNonPositive(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("값만으로 음수 조건을 검사하면 조건을 만족한 Long을 반환한다")
    void returnsLongWhenNegativeConditionMatches() {
        assertThat(NumberPrecondition.requireNegative(-1L)).isEqualTo(-1L);
    }

    @Test
    @DisplayName("값만으로 음수 조건을 검사하면 조건을 위반한 Long에 예외를 던진다")
    void throwsWhenLongViolatesNegativeCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNegative(0L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be negative.");
        assertThatThrownBy(() -> NumberPrecondition.requireNegative(1L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be negative.");
    }

    @Test
    @DisplayName("값만으로 0 이상 조건을 검사하면 조건을 만족한 Long을 반환한다")
    void returnsLongWhenNonNegativeConditionMatches() {
        assertThat(NumberPrecondition.requireNonNegative(0L)).isEqualTo(0L);
        assertThat(NumberPrecondition.requireNonNegative(1L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("값만으로 0 이상 조건을 검사하면 음수 Long에 예외를 던진다")
    void throwsWhenLongViolatesNonNegativeCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNonNegative(-1L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be non-negative.");
    }

    @Test
    @DisplayName("값만으로 양수 조건을 검사하면 조건을 만족한 Long을 반환한다")
    void returnsLongWhenPositiveConditionMatches() {
        assertThat(NumberPrecondition.requirePositive(1L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("값만으로 양수 조건을 검사하면 조건을 위반한 Long에 예외를 던진다")
    void throwsWhenLongViolatesPositiveCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requirePositive(0L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
        assertThatThrownBy(() -> NumberPrecondition.requirePositive(-1L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
    }

    @Test
    @DisplayName("값만으로 0 이하 조건을 검사하면 조건을 만족한 Long을 반환한다")
    void returnsLongWhenNonPositiveConditionMatches() {
        assertThat(NumberPrecondition.requireNonPositive(-1L)).isEqualTo(-1L);
        assertThat(NumberPrecondition.requireNonPositive(0L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("값만으로 0 이하 조건을 검사하면 양수 Long에 예외를 던진다")
    void throwsWhenLongViolatesNonPositiveCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireNonPositive(1L))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be non-positive.");
    }

    @Test
    @DisplayName("Long Negative 검사의 값이 null이면 예외를 던진다")
    void throwsWhenLongNegativeValueIsNull() {
        Long value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNegative(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Long NonNegative 검사의 값이 null이면 예외를 던진다")
    void throwsWhenLongNonNegativeValueIsNull() {
        Long value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNonNegative(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Long Positive 검사의 값이 null이면 예외를 던진다")
    void throwsWhenLongPositiveValueIsNull() {
        Long value = null;

        assertThatThrownBy(() -> NumberPrecondition.requirePositive(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Long NonPositive 검사의 값이 null이면 예외를 던진다")
    void throwsWhenLongNonPositiveValueIsNull() {
        Long value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireNonPositive(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("값만으로 최소 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenAtLeastConditionMatches() {
        assertThat(NumberPrecondition.requireAtLeast(1, 1)).isEqualTo(1);
        assertThat(NumberPrecondition.requireAtLeast(2, 1)).isEqualTo(2);
    }

    @Test
    @DisplayName("값만으로 최소 조건을 검사하면 최솟값보다 작은 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesAtLeastCondition() {
        Integer value = 1;

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> NumberPrecondition.requireAtLeast(value, 3));

        assertThat(exception).hasMessage("must be at least 3.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_LEAST);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of(3));
    }

    @Test
    @DisplayName("값만으로 최대 조건을 검사하면 조건을 만족한 Integer를 반환한다")
    void returnsIntegerWhenAtMostConditionMatches() {
        assertThat(NumberPrecondition.requireAtMost(1, 1)).isEqualTo(1);
        assertThat(NumberPrecondition.requireAtMost(0, 1)).isEqualTo(0);
    }

    @Test
    @DisplayName("값만으로 최대 조건을 검사하면 최댓값보다 큰 Integer에 예외를 던진다")
    void throwsWhenIntegerViolatesAtMostCondition() {
        assertThatThrownBy(() -> NumberPrecondition.requireAtMost(2, 1))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 1.");
    }

    @ParameterizedTest
    @CsvSource({"1, 1, 3", "2, 1, 3", "3, 1, 3", "1, 1, 1"})
    @DisplayName("Integer가 양 끝 경계를 포함한 범위 안에 있으면 값을 반환한다")
    void returnsIntegerWhenBetweenConditionMatches(int value, int minimum, int maximum) {
        assertThat(NumberPrecondition.requireBetween(value, minimum, maximum)).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    @DisplayName("Integer가 범위의 하한 또는 상한을 벗어나면 범위 정보를 담은 예외를 던진다")
    void throwsWhenIntegerIsOutOfRange(int value) {
        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> NumberPrecondition.requireBetween(value, 1, 3));

        assertThat(exception).hasMessage("must be between 1 and 3.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.BETWEEN);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of(1, 3));
    }

    @Test
    @DisplayName("값만으로 범위 조건을 검사하면 최솟값이 최댓값보다 클 때 기준 정의 오류 예외를 던진다")
    void throwsWhenIntegerBoundsAreInvalid() {
        IllegalBoundsException exception =
                catchThrowableOfType(
                        IllegalBoundsException.class,
                        () -> NumberPrecondition.requireBetween(1, 3, 1));

        assertThat(exception).hasMessage("minimum must be less than or equal to maximum.");
        assertThat(exception.minimum()).isEqualTo(3);
        assertThat(exception.maximum()).isEqualTo(1);
    }

    @Test
    @DisplayName("Integer AtLeast 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerAtLeastValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireAtLeast(value, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer AtMost 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerAtMostValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireAtMost(value, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer Between 검사의 값이 null이면 예외를 던진다")
    void throwsWhenIntegerBetweenValueIsNull() {
        Integer value = null;

        assertThatThrownBy(() -> NumberPrecondition.requireBetween(value, 1, 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Integer AtLeast 검사의 최솟값이 null이면 예외를 던진다")
    void throwsWhenIntegerAtLeastMinimumIsNull() {
        assertThatThrownBy(() -> NumberPrecondition.requireAtLeast(1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("minimum must not be null.");
    }

    @Test
    @DisplayName("Integer Between 검사의 최솟값이 null이면 예외를 던진다")
    void throwsWhenIntegerBetweenMinimumIsNull() {
        assertThatThrownBy(() -> NumberPrecondition.requireBetween(1, null, 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("minimum must not be null.");
    }

    @Test
    @DisplayName("Integer AtMost 검사의 최댓값이 null이면 예외를 던진다")
    void throwsWhenIntegerAtMostMaximumIsNull() {
        assertThatThrownBy(() -> NumberPrecondition.requireAtMost(1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maximum must not be null.");
    }

    @Test
    @DisplayName("Integer Between 검사의 최댓값이 null이면 예외를 던진다")
    void throwsWhenIntegerBetweenMaximumIsNull() {
        assertThatThrownBy(() -> NumberPrecondition.requireBetween(1, 1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maximum must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2})
    @DisplayName("Long이 AtLeast 조건을 만족하면 값을 반환한다")
    void returnsLongWhenAtLeastConditionMatches(long value) {
        assertThat(NumberPrecondition.requireAtLeast(value, 1L)).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("Long이 AtMost 조건을 만족하면 값을 반환한다")
    void returnsLongWhenAtMostConditionMatches(long value) {
        assertThat(NumberPrecondition.requireAtMost(value, 1L)).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    @DisplayName("Long이 Between 조건을 만족하면 값을 반환한다")
    void returnsLongWhenBetweenConditionMatches(long value) {
        assertThat(NumberPrecondition.requireBetween(value, 1L, 3L)).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {0})
    @DisplayName("Long이 AtLeast 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenLongViolatesAtLeastCondition(long value) {
        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> NumberPrecondition.requireAtLeast(value, 1L));

        assertThat(exception).hasMessage("must be at least 1.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_LEAST);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of(1L));
    }

    @ParameterizedTest
    @ValueSource(longs = {2})
    @DisplayName("Long이 AtMost 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenLongViolatesAtMostCondition(long value) {
        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> NumberPrecondition.requireAtMost(value, 1L));

        assertThat(exception).hasMessage("must be at most 1.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of(1L));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 4})
    @DisplayName("Long이 Between 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenLongViolatesBetweenCondition(long value) {
        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> NumberPrecondition.requireBetween(value, 1L, 3L));

        assertThat(exception).hasMessage("must be between 1 and 3.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.BETWEEN);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of(1L, 3L));
    }
}
