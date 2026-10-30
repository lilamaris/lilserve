package com.lilamaris.lilserve.kernel.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("커서")
class CursorTest {
    @Test
    @DisplayName("정적 팩토리에 생성 시각과 ID를 전달하면 같은 위치의 커서를 반환한다")
    void createsCursorWithFactory() {
        assertThat(Cursor.of(CREATED_AT, ID)).isEqualTo(new Cursor<>(CREATED_AT, ID));
    }

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("생성 시각과 ID가 주어지면 커서 위치를 보관한다")
    void retainsPosition() {
        var cursor = new Cursor<>(CREATED_AT, ID);
        assertThat(cursor.createdAt()).isEqualTo(CREATED_AT);
        assertThat(cursor.id()).isEqualTo(ID);
    }

    @Test
    @DisplayName("생성 시각이 null이면 커서를 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> new Cursor<>(null, ID)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ID가 null이면 커서를 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(() -> new Cursor<>(CREATED_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
