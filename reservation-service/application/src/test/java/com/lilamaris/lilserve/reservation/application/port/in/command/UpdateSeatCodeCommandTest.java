package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("좌석 코드 변경 커맨드")
class UpdateSeatCodeCommandTest {
    private static final SeatId ID =
            SeatId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
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
    @DisplayName("팩토리는 식별자를 보관하고 유효한 요청을 생성한다")
    void createsValidRequest() {
        var request = UpdateSeatCodeCommand.of(ID, "CODE-01");

        assertThat(request.seatId()).isSameAs(ID);
        assertThat(request.code()).isEqualTo("CODE-01");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("팩토리로 생성한 null 식별자는 검증에서 거부한다")
    void flagsNullIdentifier() {
        var request = UpdateSeatCodeCommand.of(null, "CODE-01");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("seatId");
    }

    @Test
    @DisplayName("코드 길이 상한은 검증을 통과한다")
    void acceptsCodeAtLengthLimit() {
        var request = UpdateSeatCodeCommand.of(ID, "a".repeat(Seat.MAX_CODE_LENGTH));

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("null 또는 공백 코드는 생성 후 검증에서 거부한다")
    void flagsBlankCode(String code) {
        var request = UpdateSeatCodeCommand.of(ID, code);

        assertThat(request.code()).isEqualTo(code);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("code");
    }

    @Test
    @DisplayName("길이 상한을 초과한 코드는 생성 후 검증에서 거부한다")
    void flagsCodeAboveLengthLimit() {
        var request = UpdateSeatCodeCommand.of(ID, "a".repeat(Seat.MAX_CODE_LENGTH + 1));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("code");
    }
}
