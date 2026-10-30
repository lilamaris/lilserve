package com.lilamaris.lilserve.kernel.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("커서 페이지")
class CursorPageTest {
    @Test
    @DisplayName("다음 커서가 있으면 항목과 커서를 보관하고 다음 페이지가 있다고 반환한다")
    void retainsItemsAndNextCursor() {
        var cursor = Cursor.of(Instant.parse("2026-01-01T00:00:00Z"), 1L);
        var page = CursorPage.of(List.of("post"), cursor);
        assertThat(page.items()).containsExactly("post");
        assertThat(page.nextCursor()).isEqualTo(cursor);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 커서가 없으면 마지막 페이지로 반환한다")
    void identifiesLastPage() {
        var page = CursorPage.of(List.of("post"), null);
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 마지막 페이지를 생성한다")
    void allowsEmptyPage() {
        var page = CursorPage.of(List.of(), null);
        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("원본 목록을 변경해도 페이지 항목은 변경되지 않는다")
    void copiesItems() {
        var items = new ArrayList<>(List.of("post"));
        var page = CursorPage.of(items, null);
        items.clear();
        assertThat(page.items()).containsExactly("post");
    }

    @Test
    @DisplayName("반환된 항목 목록은 수정할 수 없다")
    void exposesUnmodifiableItems() {
        var page = CursorPage.of(List.of("post"), null);
        assertThatThrownBy(() -> page.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("항목 목록이 null이면 페이지를 생성할 수 없다")
    void rejectsNullItems() {
        assertThatThrownBy(() -> CursorPage.of(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("목록에 null 항목이 있으면 페이지를 생성할 수 없다")
    void rejectsNullItem() {
        assertThatThrownBy(() -> CursorPage.of(Arrays.asList("post", null), null))
                .isInstanceOf(NullPointerException.class);
    }

    private static Cursor<Integer> cursorFor(Integer id) {
        return Cursor.of(Instant.parse("2026-01-01T00:00:00Z"), id);
    }

    @Test
    @DisplayName("초과 조회한 항목은 제외하고 반환할 마지막 항목으로 다음 커서를 만든다")
    void buildsCursorFromLastReturnedItem() {
        var page = CursorPage.from(List.of(3, 2, 1), 2, CursorPageTest::cursorFor);
        assertThat(page.items()).containsExactly(3, 2);
        assertThat(page.nextCursor()).isEqualTo(cursorFor(2));
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("조회 크기가 1이면 첫 항목만 반환하고 해당 항목의 커서를 만든다")
    void supportsSingleItemPage() {
        var page = CursorPage.from(List.of(2, 1), 1, CursorPageTest::cursorFor);
        assertThat(page.items()).containsExactly(2);
        assertThat(page.nextCursor()).isEqualTo(cursorFor(2));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, Integer.MAX_VALUE - 1})
    @DisplayName("조회 결과가 제한 개수 이하이면 커서를 추출하지 않고 마지막 페이지를 반환한다")
    void buildsLastPage(int limit) {
        var page =
                CursorPage.from(
                        List.of(2, 1),
                        limit,
                        item -> {
                            throw new AssertionError(
                                    "the cursor is not extracted on the last page");
                        });
        assertThat(page.items()).containsExactly(2, 1);
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("빈 조회 결과이면 커서를 추출하지 않고 빈 마지막 페이지를 반환한다")
    void buildsEmptyPage() {
        var page =
                CursorPage.from(
                        List.<Integer>of(),
                        2,
                        item -> {
                            throw new AssertionError(
                                    "the cursor is not extracted on an empty page");
                        });
        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("페이지 조립 후 원본 목록을 변경해도 반환 항목은 유지된다")
    void copiesFetchedItems() {
        var fetched = new ArrayList<>(List.of(3, 2, 1));
        var page = CursorPage.from(fetched, 2, CursorPageTest::cursorFor);
        fetched.clear();
        assertThat(page.items()).containsExactly(3, 2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("제한 개수가 양수가 아니면 페이지 조립을 거부한다")
    void rejectsInvalidLimit(int limit) {
        assertThatThrownBy(() -> CursorPage.from(List.of(1), limit, CursorPageTest::cursorFor))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("조회 결과가 null이면 페이지 조립을 거부한다")
    void rejectsNullFetched() {
        assertThatThrownBy(() -> CursorPage.from(null, 2, CursorPageTest::cursorFor))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("커서 추출 함수가 null이면 페이지 조립을 거부한다")
    void rejectsNullExtractor() {
        assertThatThrownBy(() -> CursorPage.from(List.of(1), 2, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("다음 페이지가 있는데 추출된 커서가 null이면 페이지 조립을 거부한다")
    void rejectsNullExtractedCursor() {
        assertThatThrownBy(() -> CursorPage.from(List.of(2, 1), 1, item -> null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("제한 개수에 추가 조회 한 개를 더할 수 없으면 페이지 조립을 거부한다")
    void rejectsLimitWithoutLookaheadCapacity() {
        assertThatThrownBy(
                        () ->
                                CursorPage.from(
                                        List.of(1), Integer.MAX_VALUE, CursorPageTest::cursorFor))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }
}
