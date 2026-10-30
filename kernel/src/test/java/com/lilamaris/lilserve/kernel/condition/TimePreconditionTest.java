package com.lilamaris.lilserve.kernel.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilserve.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("시간 조건 검사")
class TimePreconditionTest {
    private static final Instant REFERENCE = Instant.parse("2026-01-02T00:00:00Z");
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 1, 2);

    @ParameterizedTest
    @ValueSource(longs = {-1, -1_000_000_000})
    @DisplayName("Duration이 음수 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenNegativeConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requireNegative(value)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("Duration이 음수 조건을 위반하면 조건과 값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesNegativeCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> TimePrecondition.requireNegative(value));

        assertThat(exception).hasMessage("must be negative.");
        assertThat(exception.condition()).isEqualTo(TimeCondition.NEGATIVE);
        assertThat(exception.subject()).isSameAs(value);
    }

    @Test
    @DisplayName("Duration 음수 조건의 값이 null이면 예외를 던진다")
    void throwsWhenNegativeDurationIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireNegative(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("Duration이 0 이상 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenNonNegativeConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requireNonNegative(value)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1})
    @DisplayName("Duration이 0 이상 조건을 위반하면 조건과 값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesNonNegativeCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> TimePrecondition.requireNonNegative(value));

        assertThat(exception).hasMessage("must be non-negative.");
        assertThat(exception.condition()).isEqualTo(TimeCondition.NON_NEGATIVE);
        assertThat(exception.subject()).isSameAs(value);
    }

    @Test
    @DisplayName("Duration 0 이상 조건의 값이 null이면 예외를 던진다")
    void throwsWhenNonNegativeDurationIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireNonNegative(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 1_000_000_000})
    @DisplayName("Duration이 양수 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenPositiveConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requirePositive(value)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("Duration이 양수 조건을 위반하면 조건과 값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesPositiveCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> TimePrecondition.requirePositive(value));

        assertThat(exception).hasMessage("must be positive.");
        assertThat(exception.condition()).isEqualTo(TimeCondition.POSITIVE);
        assertThat(exception.subject()).isSameAs(value);
    }

    @Test
    @DisplayName("Duration 양수 조건의 값이 null이면 예외를 던진다")
    void throwsWhenPositiveDurationIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requirePositive(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("Duration이 0 이하 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenNonPositiveConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requireNonPositive(value)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {1})
    @DisplayName("Duration이 0 이하 조건을 위반하면 조건과 값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesNonPositiveCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> TimePrecondition.requireNonPositive(value));

        assertThat(exception).hasMessage("must be non-positive.");
        assertThat(exception.condition()).isEqualTo(TimeCondition.NON_POSITIVE);
        assertThat(exception.subject()).isSameAs(value);
    }

    @Test
    @DisplayName("Duration 0 이하 조건의 값이 null이면 예외를 던진다")
    void throwsWhenNonPositiveDurationIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireNonPositive(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1})
    @DisplayName("LocalDate가 이전 조건을 만족하면 LocalDate 타입의 원래 값을 반환한다")
    void returnsLocalDateWhenBeforeConditionMatches(long offset) {
        LocalDate subject = REFERENCE_DATE.plusDays(offset);

        LocalDate result = TimePrecondition.requireBefore(subject, REFERENCE_DATE);

        assertThat(result).isSameAs(subject);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("Instant가 이전 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenInstantViolatesBeforeCondition(long offset) {
        Instant subject = REFERENCE.plusNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireBefore(subject, REFERENCE));

        assertThat(exception).hasMessage("must be before 2026-01-02T00:00:00Z.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.BEFORE);
        assertThat(exception.subject()).isSameAs(subject);
        assertThat(exception.references()).isEqualTo(List.of(REFERENCE));
    }

    @Test
    @DisplayName("이전 조건의 검사 대상이 null이면 예외를 던진다")
    void throwsWhenBeforeSubjectIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireBefore((Instant) null, REFERENCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null.");
    }

    @Test
    @DisplayName("이전 조건의 기준값이 null이면 예외를 던진다")
    void throwsWhenBeforeReferenceIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireBefore(REFERENCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("upperBound must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("LocalDate가 이전 또는 같음 조건을 만족하면 LocalDate 타입의 원래 값을 반환한다")
    void returnsLocalDateWhenBeforeOrEqualConditionMatches(long offset) {
        LocalDate subject = REFERENCE_DATE.plusDays(offset);

        LocalDate result = TimePrecondition.requireBeforeOrEqual(subject, REFERENCE_DATE);

        assertThat(result).isSameAs(subject);
    }

    @ParameterizedTest
    @ValueSource(longs = {1})
    @DisplayName("Instant가 이전 또는 같음 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenInstantViolatesBeforeOrEqualCondition(long offset) {
        Instant subject = REFERENCE.plusNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireBeforeOrEqual(subject, REFERENCE));

        assertThat(exception).hasMessage("must be before or equal to 2026-01-02T00:00:00Z.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.BEFORE_OR_EQUAL);
        assertThat(exception.subject()).isSameAs(subject);
        assertThat(exception.references()).isEqualTo(List.of(REFERENCE));
    }

    @Test
    @DisplayName("이전 또는 같음 조건의 검사 대상이 null이면 예외를 던진다")
    void throwsWhenBeforeOrEqualSubjectIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireBeforeOrEqual((Instant) null, REFERENCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null.");
    }

    @Test
    @DisplayName("이전 또는 같음 조건의 기준값이 null이면 예외를 던진다")
    void throwsWhenBeforeOrEqualReferenceIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireBeforeOrEqual(REFERENCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("upperBound must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {1})
    @DisplayName("LocalDate가 이후 조건을 만족하면 LocalDate 타입의 원래 값을 반환한다")
    void returnsLocalDateWhenAfterConditionMatches(long offset) {
        LocalDate subject = REFERENCE_DATE.plusDays(offset);

        LocalDate result = TimePrecondition.requireAfter(subject, REFERENCE_DATE);

        assertThat(result).isSameAs(subject);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("Instant가 이후 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenInstantViolatesAfterCondition(long offset) {
        Instant subject = REFERENCE.plusNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireAfter(subject, REFERENCE));

        assertThat(exception).hasMessage("must be after 2026-01-02T00:00:00Z.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER);
        assertThat(exception.subject()).isSameAs(subject);
        assertThat(exception.references()).isEqualTo(List.of(REFERENCE));
    }

    @Test
    @DisplayName("이후 조건의 검사 대상이 null이면 예외를 던진다")
    void throwsWhenAfterSubjectIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAfter((Instant) null, REFERENCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null.");
    }

    @Test
    @DisplayName("이후 조건의 기준값이 null이면 예외를 던진다")
    void throwsWhenAfterReferenceIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAfter(REFERENCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lowerBound must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("LocalDate가 이후 또는 같음 조건을 만족하면 LocalDate 타입의 원래 값을 반환한다")
    void returnsLocalDateWhenAfterOrEqualConditionMatches(long offset) {
        LocalDate subject = REFERENCE_DATE.plusDays(offset);

        LocalDate result = TimePrecondition.requireAfterOrEqual(subject, REFERENCE_DATE);

        assertThat(result).isSameAs(subject);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1})
    @DisplayName("Instant가 이후 또는 같음 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenInstantViolatesAfterOrEqualCondition(long offset) {
        Instant subject = REFERENCE.plusNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireAfterOrEqual(subject, REFERENCE));

        assertThat(exception).hasMessage("must be after or equal to 2026-01-02T00:00:00Z.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AFTER_OR_EQUAL);
        assertThat(exception.subject()).isSameAs(subject);
        assertThat(exception.references()).isEqualTo(List.of(REFERENCE));
    }

    @Test
    @DisplayName("이후 또는 같음 조건의 검사 대상이 null이면 예외를 던진다")
    void throwsWhenAfterOrEqualSubjectIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAfterOrEqual((Instant) null, REFERENCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject must not be null.");
    }

    @Test
    @DisplayName("이후 또는 같음 조건의 기준값이 null이면 예외를 던진다")
    void throwsWhenAfterOrEqualReferenceIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAfterOrEqual(REFERENCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lowerBound must not be null.");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("Duration이 최소 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenAtLeastConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requireAtLeast(value, Duration.ZERO)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1})
    @DisplayName("Duration이 최소 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesAtLeastCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireAtLeast(value, Duration.ZERO));

        assertThat(exception).hasMessage("must be at least PT0S.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_LEAST);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references()).isEqualTo(List.of(Duration.ZERO));
    }

    @Test
    @DisplayName("LocalDateTime 최소 검사 결과를 LocalDateTime 타입으로 반환한다")
    void preservesLocalDateTimeTypeForAtLeastCondition() {
        LocalDateTime value = REFERENCE_DATE.atStartOfDay();

        LocalDateTime result = TimePrecondition.requireAtLeast(value, value);

        assertThat(result).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("Duration이 최대 조건을 만족하면 원래 값을 반환한다")
    void returnsDurationWhenAtMostConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(TimePrecondition.requireAtMost(value, Duration.ZERO)).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {1})
    @DisplayName("Duration이 최대 조건을 위반하면 기준값을 담은 예외를 던진다")
    void throwsWhenDurationViolatesAtMostCondition(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> TimePrecondition.requireAtMost(value, Duration.ZERO));

        assertThat(exception).hasMessage("must be at most PT0S.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references()).isEqualTo(List.of(Duration.ZERO));
    }

    @Test
    @DisplayName("LocalDateTime 최대 검사 결과를 LocalDateTime 타입으로 반환한다")
    void preservesLocalDateTimeTypeForAtMostCondition() {
        LocalDateTime value = REFERENCE_DATE.atStartOfDay();

        LocalDateTime result = TimePrecondition.requireAtMost(value, value);

        assertThat(result).isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    @DisplayName("Duration이 양 끝 경계를 포함한 범위 안에 있으면 원래 값을 반환한다")
    void returnsDurationWhenBetweenConditionMatches(long offset) {
        Duration value = Duration.ofNanos(offset);

        assertThat(
                        TimePrecondition.requireBetween(
                                value, Duration.ofNanos(-1), Duration.ofNanos(1)))
                .isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {-2, 2})
    @DisplayName("Duration이 범위의 하한 또는 상한을 벗어나면 예외를 던진다")
    void throwsWhenDurationIsOutOfRange(long offset) {
        Duration value = Duration.ofNanos(offset);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () ->
                                TimePrecondition.requireBetween(
                                        value, Duration.ofNanos(-1), Duration.ofNanos(1)));

        assertThat(exception).hasMessage("must be between PT-0.000000001S and PT0.000000001S.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.BETWEEN);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references())
                .isEqualTo(List.of(Duration.ofNanos(-1), Duration.ofNanos(1)));
    }

    @Test
    @DisplayName("Duration 범위의 최솟값과 최댓값이 같으면 같은 값을 허용한다")
    void acceptsDurationWhenBothBoundsAreEqual() {
        assertThat(TimePrecondition.requireBetween(Duration.ZERO, Duration.ZERO, Duration.ZERO))
                .isSameAs(Duration.ZERO);
    }

    @Test
    @DisplayName("Duration 범위의 최솟값이 최댓값보다 크면 기준 정의 오류를 던진다")
    void throwsWhenDurationBoundsAreInvalid() {
        IllegalBoundsException exception =
                catchThrowableOfType(
                        IllegalBoundsException.class,
                        () ->
                                TimePrecondition.requireBetween(
                                        Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(-1)));

        assertThat(exception).hasMessage("minimum must be less than or equal to maximum.");
        assertThat(exception.minimum()).isEqualTo(Duration.ofNanos(1));
        assertThat(exception.maximum()).isEqualTo(Duration.ofNanos(-1));
    }

    @Test
    @DisplayName("LocalDateTime 범위 검사 결과를 LocalDateTime 타입으로 반환한다")
    void preservesLocalDateTimeTypeForBetweenCondition() {
        LocalDateTime value = REFERENCE_DATE.atStartOfDay();

        LocalDateTime result =
                TimePrecondition.requireBetween(value, value.minusDays(1), value.plusDays(1));

        assertThat(result).isSameAs(value);
    }

    @Test
    @DisplayName("AtLeast 검사의 값이 null이면 예외를 던진다")
    void throwsWhenAtLeastValueIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAtLeast(null, Duration.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("AtLeast 검사의 최솟값이 null이면 예외를 던진다")
    void throwsWhenAtLeastMinimumIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAtLeast(Duration.ZERO, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("minimum must not be null.");
    }

    @Test
    @DisplayName("AtMost 검사의 값이 null이면 예외를 던진다")
    void throwsWhenAtMostValueIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAtMost(null, Duration.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("AtMost 검사의 최댓값이 null이면 예외를 던진다")
    void throwsWhenAtMostMaximumIsNull() {
        assertThatThrownBy(() -> TimePrecondition.requireAtMost(Duration.ZERO, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maximum must not be null.");
    }

    @Test
    @DisplayName("Between 검사의 값이 null이면 예외를 던진다")
    void throwsWhenBetweenValueIsNull() {
        assertThatThrownBy(
                        () -> TimePrecondition.requireBetween(null, Duration.ZERO, Duration.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("Between 검사의 최솟값이 null이면 예외를 던진다")
    void throwsWhenBetweenMinimumIsNull() {
        assertThatThrownBy(
                        () -> TimePrecondition.requireBetween(Duration.ZERO, null, Duration.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("minimum must not be null.");
    }

    @Test
    @DisplayName("Between 검사의 최댓값이 null이면 예외를 던진다")
    void throwsWhenBetweenMaximumIsNull() {
        assertThatThrownBy(
                        () -> TimePrecondition.requireBetween(Duration.ZERO, Duration.ZERO, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("maximum must not be null.");
    }
}
