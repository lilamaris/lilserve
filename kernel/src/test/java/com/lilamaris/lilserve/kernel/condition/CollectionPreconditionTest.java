package com.lilamaris.lilserve.kernel.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.lilamaris.lilserve.kernel.exception.IllegalBoundsException;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Collection Precondition 테스트")
class CollectionPreconditionTest {
    @Test
    @DisplayName("값만으로 null 요소 조건을 검사하면 조건을 만족한 컬렉션을 반환한다")
    void returnsCollectionWhenNullElementConditionMatches() {
        List<String> value = List.of("a", "b");

        assertThat(CollectionPrecondition.requireNonNullElements(value)).isSameAs(value);
        assertThat(CollectionPrecondition.requireNonNullElements(List.of())).isEqualTo(List.of());
    }

    @Test
    @DisplayName("값만으로 null 요소 조건을 검사하면 null 요소를 포함한 컬렉션에 예외를 던진다")
    void throwsWhenCollectionViolatesNullElementCondition() {
        List<String> value = Arrays.asList("a", null);

        IllegalValueConditionException exception =
                catchThrowableOfType(
                        IllegalValueConditionException.class,
                        () -> CollectionPrecondition.requireNonNullElements(value));

        assertThat(exception).hasMessage("must not contain null elements.");
        assertThat(exception.condition()).isEqualTo(CollectionCondition.NULL_ELEMENT);
        assertThat(exception.subject()).isSameAs(value);
    }

    @Test
    @DisplayName("값만으로 null 요소 조건을 검사하면 null 컬렉션에 NullPointerException을 던진다")
    void throwsNullPointerExceptionWhenCollectionIsNull() {
        List<String> value = null;

        assertThatThrownBy(() -> CollectionPrecondition.requireNonNullElements(value))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "2, 1", "2, 2"})
    @DisplayName("컬렉션 크기가 최솟값 이상이면 원래 컬렉션을 반환한다")
    void returnsCollectionWhenSizeIsAtLeastMinimum(int size, int minimum) {
        List<Integer> value = IntStream.range(0, size).boxed().toList();

        assertThat(CollectionPrecondition.requireSizeAtLeast(value, minimum)).isSameAs(value);
    }

    @Test
    @DisplayName("크기 최솟값을 위반하는 컬렉션을 요구하면 기준값을 담은 예외를 던진다")
    void throwsWhenSizeIsBelowMinimum() {
        List<String> value = List.of("a", "b");

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> CollectionPrecondition.requireSizeAtLeast(value, 3));

        assertThat(exception).hasMessage("must have at least 3.");
        assertThat(exception.condition()).isEqualTo(CollectionCondition.SIZE_AT_LEAST);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references()).isEqualTo(List.of(3));
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "2, 2", "2, 3"})
    @DisplayName("컬렉션 크기가 최댓값 이하이면 원래 컬렉션을 반환한다")
    void returnsCollectionWhenSizeIsAtMostMaximum(int size, int maximum) {
        List<Integer> value = IntStream.range(0, size).boxed().toList();

        assertThat(CollectionPrecondition.requireSizeAtMost(value, maximum)).isSameAs(value);
    }

    @Test
    @DisplayName("컬렉션 크기가 최댓값보다 크면 기준값을 담은 예외를 던진다")
    void throwsWhenSizeExceedsMaximum() {
        List<String> value = List.of("a", "b");

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> CollectionPrecondition.requireSizeAtMost(value, 1));

        assertThat(exception).hasMessage("must have at most 1.");
        assertThat(exception.condition()).isEqualTo(CollectionCondition.SIZE_AT_MOST);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references()).isEqualTo(List.of(1));
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 0", "1, 1, 3", "2, 1, 3", "3, 1, 3", "2, 2, 2"})
    @DisplayName("컬렉션 크기가 양 끝 경계를 포함한 범위 안에 있으면 원래 컬렉션을 반환한다")
    void returnsCollectionWhenSizeIsWithinRange(int size, int minimum, int maximum) {
        List<Integer> value = IntStream.range(0, size).boxed().toList();

        assertThat(CollectionPrecondition.requireSizeBetween(value, minimum, maximum))
                .isSameAs(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    @DisplayName("컬렉션 크기가 범위의 하한 또는 상한을 벗어나면 범위 정보를 담은 예외를 던진다")
    void throwsWhenSizeIsOutOfRange(int size) {
        List<Integer> value = IntStream.range(0, size).boxed().toList();

        IllegalComparisonConditionException exception =
                catchThrowableOfType(
                        IllegalComparisonConditionException.class,
                        () -> CollectionPrecondition.requireSizeBetween(value, 1, 3));

        assertThat(exception).hasMessage("must have between 1 and 3.");
        assertThat(exception.condition()).isEqualTo(CollectionCondition.SIZE_BETWEEN);
        assertThat(exception.subject()).isSameAs(value);
        assertThat(exception.references()).isEqualTo(List.of(1, 3));
    }

    @Test
    @DisplayName("크기 범위의 최솟값이 최댓값보다 크면 기준 정의 오류 예외를 던진다")
    void throwsWhenSizeBoundsAreInvalid() {
        IllegalBoundsException exception =
                catchThrowableOfType(
                        IllegalBoundsException.class,
                        () -> CollectionPrecondition.requireSizeBetween(List.of("a", "b"), 5, 3));

        assertThat(exception).hasMessage("minimum must be less than or equal to maximum.");
        assertThat(exception.minimum()).isEqualTo(5);
        assertThat(exception.maximum()).isEqualTo(3);
    }

    @Test
    @DisplayName("컬렉션 크기 AtLeast 검사의 값이 null이면 예외를 던진다")
    void throwsWhenSizeAtLeastCollectionIsNull() {
        List<String> value = null;

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeAtLeast(value, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("컬렉션 크기 AtMost 검사의 값이 null이면 예외를 던진다")
    void throwsWhenSizeAtMostCollectionIsNull() {
        List<String> value = null;

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeAtMost(value, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @Test
    @DisplayName("컬렉션 크기 Between 검사의 값이 null이면 예외를 던진다")
    void throwsWhenSizeBetweenCollectionIsNull() {
        List<String> value = null;

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeBetween(value, 1, 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null.");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    @DisplayName("컬렉션 크기 최솟값이 음수이면 기준 정의 오류를 던진다")
    void rejectsNegativeSizeAtLeast(int bound) {
        List<String> value = List.of("a");

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeAtLeast(value, bound))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("minimum must be non-negative.");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    @DisplayName("컬렉션 크기 최댓값이 음수이면 기준 정의 오류를 던진다")
    void rejectsNegativeSizeAtMost(int bound) {
        List<String> value = List.of("a");

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeAtMost(value, bound))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximum must be non-negative.");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    @DisplayName("컬렉션 크기 범위 최솟값이 음수이면 기준 정의 오류를 던진다")
    void rejectsNegativeSizeBetweenMinimum(int bound) {
        List<String> value = List.of("a");

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeBetween(value, bound, 3))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("minimum must be non-negative.");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    @DisplayName("컬렉션 크기 범위 최댓값이 음수이면 기준 정의 오류를 던진다")
    void rejectsNegativeSizeBetweenMaximum(int bound) {
        List<String> value = List.of("a");

        assertThatThrownBy(() -> CollectionPrecondition.requireSizeBetween(value, 0, bound))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximum must be non-negative.");
    }
}
