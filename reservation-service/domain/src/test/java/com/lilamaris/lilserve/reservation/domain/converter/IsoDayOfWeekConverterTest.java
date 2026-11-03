package com.lilamaris.lilserve.reservation.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ISO 요일 변환기")
class IsoDayOfWeekConverterTest {
    private final IsoDayOfWeekConverter converter = new IsoDayOfWeekConverter();

    @Test
    @DisplayName("월요일은 ISO 요일 1로 저장한다")
    void storesMondayAsOne() {
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.MONDAY)).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("일요일은 ISO 요일 7로 저장한다")
    void storesSundayAsSeven() {
        assertThat(converter.convertToDatabaseColumn(DayOfWeek.SUNDAY)).isEqualTo((short) 7);
    }

    @Test
    @DisplayName("ISO 요일 번호를 요일로 읽는다")
    void readsIsoNumberAsDayOfWeek() {
        assertThat(converter.convertToEntityAttribute((short) 3)).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    @DisplayName("저장과 읽기는 서로 반대이다")
    void roundTripsThroughIsoNumber() {
        for (DayOfWeek day : DayOfWeek.values()) {
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(day)))
                    .isEqualTo(day);
        }
    }

    @Test
    @DisplayName("요일이 null이면 저장할 수 없다")
    void rejectsNullDayOfWeek() {
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("요일 번호가 null이면 읽을 수 없다")
    void rejectsNullIsoNumber() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ISO 요일 번호가 1보다 작으면 읽을 수 없다")
    void rejectsIsoNumberBelowOne() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute((short) 0))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    @DisplayName("ISO 요일 번호가 7보다 크면 읽을 수 없다")
    void rejectsIsoNumberAboveSeven() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute((short) 8))
                .isInstanceOf(DateTimeException.class);
    }
}
