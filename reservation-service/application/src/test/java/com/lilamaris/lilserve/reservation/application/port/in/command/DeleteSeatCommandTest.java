package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("좌석 삭제 커맨드")
class DeleteSeatCommandTest {
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
        var request = DeleteSeatCommand.of(ID);

        assertThat(request.seatId()).isEqualTo(SeatId.of(ID));
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("팩터리에 null 식별자를 전달하면 예외가 발생한다")
    void rejectsNullIdentifierInFactory() {
        assertThatNullPointerException().isThrownBy(() -> DeleteSeatCommand.of(null));
    }

    @Test
    @DisplayName("직접 생성한 null 식별자는 검증에서 거부한다")
    void flagsNullIdentifier() {
        var request = new DeleteSeatCommand(null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("seatId");
    }
}
