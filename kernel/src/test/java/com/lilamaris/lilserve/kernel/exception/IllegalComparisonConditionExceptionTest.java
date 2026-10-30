package com.lilamaris.lilserve.kernel.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.condition.ComparisonCondition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("비교 조건 위반 예외")
class IllegalComparisonConditionExceptionTest {
    @Test
    @DisplayName("기준값이 없으면 예외 생성을 거부한다")
    void rejectsMissingReferences() {
        assertThatThrownBy(
                        () ->
                                new IllegalComparisonConditionException(
                                        0, ComparisonCondition.AT_LEAST))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one reference is required.");
    }

    @Test
    @DisplayName("기준값이 하나이면 첫 기준값과 전체 기준값을 제공한다")
    void exposesSingleReference() {
        IllegalComparisonConditionException exception =
                new IllegalComparisonConditionException(0, ComparisonCondition.AT_LEAST, 1);

        assertThat(exception).hasMessage("must be at least 1.");
        assertThat(exception.subject()).isEqualTo(0);
        assertThat(exception.condition()).isEqualTo(ComparisonCondition.AT_LEAST);
        assertThat(exception.reference()).isEqualTo(1);
        assertThat(exception.references()).isEqualTo(List.of(1));
    }

    @Test
    @DisplayName("기준값이 여러 개이면 순서대로 보관하고 첫 기준값을 제공한다")
    void preservesReferenceOrder() {
        IllegalComparisonConditionException exception =
                new IllegalComparisonConditionException(4, ComparisonCondition.BETWEEN, 1, 3);

        assertThat(exception).hasMessage("must be between 1 and 3.");
        assertThat(exception.reference()).isEqualTo(1);
        assertThat(exception.references()).isEqualTo(List.of(1, 3));
    }

    @Test
    @DisplayName("생성 시 전달한 배열을 변경해도 보관한 기준값은 바뀌지 않는다")
    void copiesReferenceArray() {
        Object[] references = {1, 3};
        IllegalComparisonConditionException exception =
                new IllegalComparisonConditionException(4, ComparisonCondition.BETWEEN, references);

        references[0] = 9;

        assertThat(exception.reference()).isEqualTo(1);
        assertThat(exception.references()).isEqualTo(List.of(1, 3));
    }

    @Test
    @DisplayName("공개된 기준값 목록을 변경할 수 없다")
    void exposesUnmodifiableReferences() {
        IllegalComparisonConditionException exception =
                new IllegalComparisonConditionException(0, ComparisonCondition.AT_LEAST, 1);

        assertThatThrownBy(() -> exception.references().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(exception.references()).isEqualTo(List.of(1));
    }

    @Test
    @DisplayName("기준값에 null이 있으면 예외 생성을 거부한다")
    void rejectsNullReference() {
        assertThatThrownBy(
                        () ->
                                new IllegalComparisonConditionException(
                                        4, ComparisonCondition.BETWEEN, 1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
