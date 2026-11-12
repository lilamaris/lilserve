package com.lilamaris.lilserve.reservation.application.port.in.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("방 목록 조회 조건")
class ListRoomQueryTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("비어 있는 이름과 코드 필터는 지정하지 않은 조건으로 정규화한다")
    void ignoresBlankFilters(String value) {
        var query = ListRoomQuery.of(value, value, OffsetPageRequest.of(0, 10));

        assertThat(query.name()).isNull();
        assertThat(query.code()).isNull();
    }

    @Test
    @DisplayName("필터 앞뒤 공백을 제거하고 실제 검색 문자열을 보존한다")
    void stripsFilterWhitespace() {
        var query = ListRoomQuery.of(" Main Hall ", " A-%_ ", OffsetPageRequest.of(0, 10));

        assertThat(query.name()).isEqualTo("Main Hall");
        assertThat(query.code()).isEqualTo("A-%_");
    }
}
