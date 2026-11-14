package com.lilamaris.lilserve.identity.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("사용자 식별자")
class UserIdTest {
    @Test
    @DisplayName("주어진 UUID를 보관한다")
    void retainsValue() {
        var value = UUID.randomUUID();

        assertThat(UserId.of(value).getValue()).isEqualTo(value);
    }

    @Test
    @DisplayName("같은 UUID를 가진 식별자는 동일하다")
    void comparesByValue() {
        var value = UUID.randomUUID();

        assertThat(UserId.of(value))
                .isEqualTo(UserId.of(value))
                .hasSameHashCodeAs(UserId.of(value));
    }

    @Test
    @DisplayName("다른 UUID를 가진 식별자는 다르다")
    void distinguishesDifferentValues() {
        assertThat(UserId.of(UUID.randomUUID())).isNotEqualTo(UserId.of(UUID.randomUUID()));
    }

    @Test
    @DisplayName("식별자 값이 없으면 생성할 수 없다")
    void rejectsNullValue() {
        assertThatThrownBy(() -> UserId.of(null)).isInstanceOf(NullPointerException.class);
    }
}
