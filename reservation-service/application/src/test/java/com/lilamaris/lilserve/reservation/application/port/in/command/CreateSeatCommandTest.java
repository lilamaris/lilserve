package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
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

@DisplayName("좌석 생성 커맨드")
class CreateSeatCommandTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
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
    @DisplayName("팩터리는 식별자를 변환하고 유효한 요청을 생성한다")
    void createsValidRequest() {
        var request = CreateSeatCommand.of(ID, "CODE-01");

        assertThat(request.roomId()).isEqualTo(RoomId.of(ID));
        assertThat(request.code()).isEqualTo("CODE-01");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("팩터리에 null 식별자를 전달하면 예외가 발생한다")
    void rejectsNullIdentifierInFactory() {
        assertThatNullPointerException().isThrownBy(() -> CreateSeatCommand.of(null, "CODE-01"));
    }

    @Test
    @DisplayName("직접 생성한 null 식별자는 검증에서 거부한다")
    void flagsNullIdentifier() {
        var request = new CreateSeatCommand(null, "CODE-01");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("roomId");
    }

    @Test
    @DisplayName("코드 길이 상한은 검증을 통과한다")
    void acceptsCodeAtLengthLimit() {
        var request = CreateSeatCommand.of(ID, "a".repeat(Seat.MAX_CODE_LENGTH));

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("null 또는 공백 코드는 생성 후 검증에서 거부한다")
    void flagsBlankCode(String code) {
        var request = CreateSeatCommand.of(ID, code);

        assertThat(request.code()).isEqualTo(code);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("code");
    }

    @Test
    @DisplayName("길이 상한을 초과한 코드는 생성 후 검증에서 거부한다")
    void flagsCodeAboveLengthLimit() {
        var request = CreateSeatCommand.of(ID, "a".repeat(Seat.MAX_CODE_LENGTH + 1));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("code");
    }
}
