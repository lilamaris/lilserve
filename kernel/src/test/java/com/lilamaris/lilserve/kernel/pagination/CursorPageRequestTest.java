package com.lilamaris.lilserve.kernel.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("커서 페이지 요청")
class CursorPageRequestTest {
    @Test
    @DisplayName("커서 없이 양수 크기를 전달하면 첫 페이지 요청을 생성한다")
    void createsFirstPageRequest() {
        var request = CursorPageRequest.of(null, 1);
        assertThat(request.cursor()).isNull();
        assertThat(request.isFirstPage()).isTrue();
        assertThat(request.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("커서와 크기를 전달하면 다음 페이지 조회 조건을 보관한다")
    void retainsCursorAndSize() {
        var cursor = Cursor.of(Instant.parse("2026-01-01T00:00:00Z"), 1L);
        var request = CursorPageRequest.of(cursor, 20);
        assertThat(request.cursor()).isEqualTo(cursor);
        assertThat(request.isFirstPage()).isFalse();
        assertThat(request.size()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("조회 크기가 0 이하이면 요청을 생성할 수 없다")
    void rejectsNonPositiveSize(int size) {
        assertThatThrownBy(() -> CursorPageRequest.of(null, size))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("크기가 정수 최댓값보다 하나 작으면 추가 조회 가능한 요청을 생성한다")
    void acceptsMaximumLookaheadSize() {
        assertThat(new CursorPageRequest<>(null, Integer.MAX_VALUE - 1).size())
                .isEqualTo(Integer.MAX_VALUE - 1);
    }

    @Test
    @DisplayName("크기에 추가 조회 한 개를 더할 수 없으면 요청을 거부한다")
    void rejectsSizeWithoutLookaheadCapacity() {
        assertThatThrownBy(() -> CursorPageRequest.of(null, Integer.MAX_VALUE))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }
}
