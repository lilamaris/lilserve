package com.lilamaris.lilserve.reservation.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;
import java.util.Objects;

/**
 * Converts {@link DayOfWeek} to its ISO weekday number, so Monday is stored as 1 and Sunday as 7.
 * The JPA default stores an enum ordinal, which is one below the ISO number and shifts every
 * weekday-based comparison in SQL, so this converter is applied explicitly to each weekday column.
 * The database type is {@code SMALLINT} to match the schema columns.
 */
@Converter
public class IsoDayOfWeekConverter implements AttributeConverter<DayOfWeek, Short> {
    /**
     * Converts a weekday to its ISO number.
     *
     * @param attribute the weekday to convert
     * @return the ISO number, from 1 for Monday to 7 for Sunday
     * @throws NullPointerException if {@code attribute} is null
     */
    @Override
    public Short convertToDatabaseColumn(DayOfWeek attribute) {
        return (short) Objects.requireNonNull(attribute, "attribute must not be null.").getValue();
    }

    /**
     * Converts an ISO weekday number to its weekday.
     *
     * @param dbData the ISO number, from 1 to 7
     * @return the matching weekday
     * @throws NullPointerException if {@code dbData} is null
     * @throws java.time.DateTimeException if {@code dbData} is outside 1 to 7
     */
    @Override
    public DayOfWeek convertToEntityAttribute(Short dbData) {
        return DayOfWeek.of(Objects.requireNonNull(dbData, "dbData must not be null."));
    }
}
