package com.lilamaris.lilserve.reservation.domain.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("예약 슬롯 식별자")
class ReservedScheduleIdTest {
    private static final UUID VALUE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_VALUE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("값을 전달하면 식별자 값을 보관한다")
    void retainsValue() {
        assertThat(ReservedScheduleId.of(VALUE).getValue()).isEqualTo(VALUE);
    }

    @Test
    @DisplayName("값이 null이면 예약 슬롯 식별자를 생성할 수 없다")
    void rejectsNullValue() {
        assertThatThrownBy(() -> ReservedScheduleId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("같은 값을 가진 예약 슬롯 식별자는 같다")
    void equalsSameValue() {
        assertThat(ReservedScheduleId.of(VALUE))
                .isEqualTo(ReservedScheduleId.of(VALUE))
                .hasSameHashCodeAs(ReservedScheduleId.of(VALUE));
    }

    @Test
    @DisplayName("다른 값을 가진 예약 슬롯 식별자는 같지 않다")
    void isNotEqualToOtherValue() {
        assertThat(ReservedScheduleId.of(VALUE)).isNotEqualTo(ReservedScheduleId.of(OTHER_VALUE));
    }
}
