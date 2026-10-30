package com.lilamaris.lilserve.kernel.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("String Precondition 테스트")
class StringPreconditionTest {
    @Test
    @DisplayName("값만으로 빈 문자열 조건을 검사하면 조건을 만족한 값을 반환한다")
    void returnsValueWhenNonBlankConditionMatches() {
        assertThat(StringPrecondition.requireNonBlank("value")).isEqualTo("value");
    }

    @Test
    @DisplayName("값만으로 빈 문자열 조건을 검사하면 빈 문자열에 예외를 던진다")
    void throwsWhenValueViolatesNonBlankCondition() {
        String value = " ";

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> StringPrecondition.requireNonBlank(value));

        assertThat(exception).hasMessage("must not be blank.");
        assertThat(exception.condition()).isEqualTo(StringCondition.NOT_BLANK);
        assertThat(exception.subject()).isEqualTo(value);
    }

    @Test
    @DisplayName("값만으로 빈 문자열 조건을 검사하면 null 값에 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenValueIsNull() {
        String value = null;

        assertThatThrownBy(() -> StringPrecondition.requireNonBlank(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("값만으로 포함 조건을 검사하면 조건을 만족한 값을 반환한다")
    void returnsValueWhenNotContainConditionMatches() {
        assertThat(StringPrecondition.requireNotContain("text", "z")).isEqualTo("text");
    }

    @Test
    @DisplayName("값만으로 포함 조건을 검사하면 기준 문자열을 포함한 값에 예외를 던진다")
    void throwsWhenValueContainsText() {
        String value = "text";

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> StringPrecondition.requireNotContain(value, "e"));

        assertThat(exception).hasMessage("must not contain e.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.NOT_CONTAIN);
        assertThat(exception.subject()).isEqualTo(value);
        assertThat(exception.references()).isEqualTo(List.of("e"));
    }

    @Test
    @DisplayName("값만으로 포함 조건을 검사하면 값이 빈 문자열일 때 예외를 던진다")
    void throwsWhenValueIsBlankForNotContainCondition() {
        assertThatThrownBy(() -> StringPrecondition.requireNotContain(" ", "x"))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");
    }

    @Test
    @DisplayName("값만으로 포함 조건을 검사하면 값이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenValueIsNullForNotContainCondition() {
        String value = null;

        assertThatThrownBy(() -> StringPrecondition.requireNotContain(value, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("값만으로 포함 조건을 검사하면 기준 문자열이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenTextIsNull() {
        assertThatThrownBy(() -> StringPrecondition.requireNotContain("text", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("text must not be null.");
    }

    @Test
    @DisplayName("최대 길이 조건을 검사하면 조건을 만족한 값을 반환한다")
    void returnsValueWhenMaxLenConditionMatches() {
        assertThat(StringPrecondition.requireMaxLen("value", 10)).isEqualTo("value");
    }

    @Test
    @DisplayName("최대 길이 조건을 검사하면 값 길이가 최대 길이와 같으면 값을 그대로 반환한다")
    void returnsValueWhenValueLengthEqualsMaxLen() {
        assertThat(StringPrecondition.requireMaxLen("a".repeat(5), 5)).isEqualTo("a".repeat(5));
    }

    @Test
    @DisplayName("최대 길이 조건을 검사하면 값 길이가 최대 길이를 넘으면 예외를 던진다")
    void throwsWhenValueExceedsMaxLen() {
        String value = "a".repeat(6);

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> StringPrecondition.requireMaxLen(value, 5));

        assertThat(exception).hasMessage("must be at most 5.");
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_MOST);
        assertThat(exception.subject()).isEqualTo(6);
        assertThat(exception.reference()).isEqualTo(5);
    }

    @Test
    @DisplayName("최대 길이 조건을 검사하면 값이 빈 문자열일 때 예외를 던진다")
    void throwsWhenValueIsBlankForMaxLenCondition() {
        assertThatThrownBy(() -> StringPrecondition.requireMaxLen(" ", 5))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must not be blank.");
    }

    @Test
    @DisplayName("최대 길이 조건을 검사하면 값이 null일 때 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenValueIsNullForMaxLenCondition() {
        assertThatThrownBy(() -> StringPrecondition.requireMaxLen(null, 5))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }
}
