package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("스케줄 수정 커맨드")
class UpdateScheduleCommandTest {
    private static final ScheduleId ID = ScheduleId.of(UUID.randomUUID());
    private static final LocalTimeRange RANGE =
            LocalTimeRange.of(LocalTime.of(10, 0), LocalTime.of(12, 0));
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("팩토리는 스케줄 식별자와 교체할 시간 범위 및 요일을 보관한다")
    void createsValidRequest() {
        var command = UpdateScheduleCommand.of(ID, RANGE, DayOfWeek.TUESDAY);

        assertThat(command.scheduleId()).isEqualTo(ID);
        assertThat(command.range()).isEqualTo(RANGE);
        assertThat(command.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(validator.validate(command)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("commandsWithMissingFields")
    @DisplayName("필수 필드가 없으면 생성 후 검증에서 해당 필드를 거부한다")
    void rejectsMissingFieldDuringValidation(UpdateScheduleCommand command, String field) {
        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly(field);
    }

    private static Stream<Arguments> commandsWithMissingFields() {
        return Stream.of(
                arguments(UpdateScheduleCommand.of(null, RANGE, DayOfWeek.MONDAY), "scheduleId"),
                arguments(UpdateScheduleCommand.of(ID, null, DayOfWeek.MONDAY), "range"),
                arguments(UpdateScheduleCommand.of(ID, RANGE, null), "dayOfWeek"));
    }
}
