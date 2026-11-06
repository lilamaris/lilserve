package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.Room;
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

@DisplayName("룸 메타데이터 변경 커맨드")
class UpdateRoomMetadataCommandTest {
    private static final RoomId ID =
            RoomId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";
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
    @DisplayName("팩토리는 식별자와 메타데이터를 그대로 보관한다")
    void createsValidCommand() {
        var command = UpdateRoomMetadataCommand.of(ID, NAME, DESCRIPTION);

        assertThat(command.roomId()).isSameAs(ID);
        assertThat(command.name()).isEqualTo(NAME);
        assertThat(command.description()).isEqualTo(DESCRIPTION);
        assertThat(validator.validate(command)).isEmpty();
    }

    @Test
    @DisplayName("팩토리로 생성한 null 식별자는 검증에서 거부한다")
    void flagsNullIdentifier() {
        var command = UpdateRoomMetadataCommand.of(null, NAME, DESCRIPTION);

        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("roomId");
    }

    @Test
    @DisplayName("이름과 설명의 길이 상한은 검증을 통과한다")
    void acceptsValuesAtLengthLimits() {
        var command =
                UpdateRoomMetadataCommand.of(
                        ID,
                        "a".repeat(Room.MAX_NAME_LENGTH),
                        "a".repeat(Room.MAX_DESCRIPTION_LENGTH));

        assertThat(validator.validate(command)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("null 또는 공백 이름은 생성 후 검증에서 거부한다")
    void flagsBlankName(String name) {
        var command = UpdateRoomMetadataCommand.of(ID, name, DESCRIPTION);

        assertThat(command.name()).isEqualTo(name);
        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("name");
    }

    @Test
    @DisplayName("길이 상한을 초과한 이름은 검증에서 거부한다")
    void flagsNameAboveLengthLimit() {
        var command =
                UpdateRoomMetadataCommand.of(ID, "a".repeat(Room.MAX_NAME_LENGTH + 1), DESCRIPTION);

        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    @DisplayName("빈 설명과 공백 설명은 검증을 통과한다")
    void acceptsBlankDescription(String description) {
        var command = UpdateRoomMetadataCommand.of(ID, NAME, description);

        assertThat(command.description()).isEqualTo(description);
        assertThat(validator.validate(command)).isEmpty();
    }

    @Test
    @DisplayName("null 설명은 생성 후 검증에서 거부한다")
    void flagsNullDescription() {
        var command = UpdateRoomMetadataCommand.of(ID, NAME, null);

        assertThat(command.description()).isNull();
        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }

    @Test
    @DisplayName("길이 상한을 초과한 설명은 검증에서 거부한다")
    void flagsDescriptionAboveLengthLimit() {
        var command =
                UpdateRoomMetadataCommand.of(ID, NAME, "a".repeat(Room.MAX_DESCRIPTION_LENGTH + 1));

        assertThat(validator.validate(command))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("description");
    }
}
