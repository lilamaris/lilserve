package com.lilamaris.lilserve.reservation.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifies blackout identifier construction, null rejection, and value-based equality. */
@DisplayName("차단 스케줄 식별자")
class BlackoutScheduleIdTest {
    private static final UUID VALUE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_VALUE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("값을 전달하면 식별자 값을 보관한다")
    void retainsValue() {
        assertThat(BlackoutScheduleId.of(VALUE).getValue()).isEqualTo(VALUE);
    }

    @Test
    @DisplayName("값이 null이면 차단 스케줄 식별자를 생성할 수 없다")
    void rejectsNullValue() {
        assertThatThrownBy(() -> BlackoutScheduleId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("같은 값을 가진 차단 스케줄 식별자는 같다")
    void equalsSameValue() {
        assertThat(BlackoutScheduleId.of(VALUE))
                .isEqualTo(BlackoutScheduleId.of(VALUE))
                .hasSameHashCodeAs(BlackoutScheduleId.of(VALUE));
    }

    @Test
    @DisplayName("다른 값을 가진 차단 스케줄 식별자는 같지 않다")
    void isNotEqualToOtherValue() {
        assertThat(BlackoutScheduleId.of(VALUE)).isNotEqualTo(BlackoutScheduleId.of(OTHER_VALUE));
    }
}
