package com.lilamaris.lilserve.kernel.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("오프셋 페이지")
class OffsetPageTest {
    @ParameterizedTest
    @CsvSource({
        "0, 20, 0, 0, false, false",
        "0, 20, 40, 2, false, true",
        "1, 20, 41, 3, true, true",
        "2, 20, 41, 3, true, false",
        "3, 20, 41, 3, true, false",
        "0, 1, 9223372036854775807, 9223372036854775807, false, true",
        "2147483647, 1, 2147483649, 2147483649, true, true"
    })
    @DisplayName("페이지 정보로 전체 페이지 수와 이전·다음 페이지 여부를 계산한다")
    void calculatesPageMetadata(
            int page, int size, long total, long totalPages, boolean previous, boolean next) {
        var result = OffsetPage.of(List.of(), page, size, total);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(total);
        assertThat(result.totalPages()).isEqualTo(totalPages);
        assertThat(result.hasPrevious()).isEqualTo(previous);
        assertThat(result.hasNext()).isEqualTo(next);
    }

    @ParameterizedTest
    @CsvSource({"-1, 20, 0", "0, 0, 0", "0, -1, 0", "0, 20, -1"})
    @DisplayName("페이지·크기·전체 개수가 허용 범위를 벗어나면 생성을 거부한다")
    void rejectsInvalidMetadata(int page, int size, long total) {
        assertThatThrownBy(() -> OffsetPage.of(List.of(), page, size, total))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("원본 목록을 변경해도 페이지의 항목은 유지된다")
    void copiesItems() {
        var items = new ArrayList<>(List.of("post"));
        var result = OffsetPage.of(items, 0, 20, 1);
        items.clear();
        assertThat(result.items()).containsExactly("post");
    }

    @Test
    @DisplayName("반환된 항목 목록은 수정할 수 없다")
    void exposesUnmodifiableItems() {
        var result = OffsetPage.of(List.of("post"), 0, 20, 1);
        assertThatThrownBy(() -> result.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("항목 수가 페이지 크기를 초과하면 생성을 거부한다")
    void rejectsTooManyItems() {
        assertThatThrownBy(() -> OffsetPage.of(List.of("A", "B"), 0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("목록이 null이면 페이지를 생성할 수 없다")
    void rejectsNullItems() {
        assertThatThrownBy(() -> OffsetPage.of(null, 0, 20, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("목록에 null 항목이 있으면 페이지를 생성할 수 없다")
    void rejectsNullItem() {
        assertThatThrownBy(() -> OffsetPage.of(Arrays.asList("A", null), 0, 20, 2))
                .isInstanceOf(NullPointerException.class);
    }
}
