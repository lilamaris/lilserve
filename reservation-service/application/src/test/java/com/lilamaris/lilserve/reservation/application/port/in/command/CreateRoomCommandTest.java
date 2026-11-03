package com.lilamaris.lilserve.reservation.application.port.in.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("룸 생성 커맨드")
class CreateRoomCommandTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final RoomId ROOM_ID = RoomId.of(ID);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";
    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("커맨드를 생성하면 구성값을 보관한다")
    void retainsGivenValues() {
        var command = CreateRoomCommand.of(CODE, NAME, DESCRIPTION);

        assertThat(command.code()).isEqualTo(CODE);
        assertThat(command.name()).isEqualTo(NAME);
        assertThat(command.description()).isEqualTo(DESCRIPTION);
    }

    @Test
    @DisplayName("구성값으로 커맨드를 비교하고 설명을 알 수 있다")
    void comparesByValues() {
        var command = CreateRoomCommand.of(CODE, NAME, DESCRIPTION);

        assertThat(command).isEqualTo(CreateRoomCommand.of(CODE, NAME, DESCRIPTION));
        assertThat(command).hasSameHashCodeAs(CreateRoomCommand.of(CODE, NAME, DESCRIPTION));
        assertThat(command).isNotEqualTo(CreateRoomCommand.of("ROOM-02", NAME, DESCRIPTION));
        assertThat(command.toString()).contains(CODE, NAME, DESCRIPTION);
    }

    @Test
    @DisplayName("허용되는 값은 검증을 통과한다")
    void passesValidationWhenValuesAreAllowed() {
        assertThat(VALIDATOR.validate(CreateRoomCommand.of(CODE, NAME, DESCRIPTION))).isEmpty();
    }

    @Test
    @DisplayName("길이 상한과 같은 값은 검증을 통과한다")
    void passesValidationAtLengthLimits() {
        var command = CreateRoomCommand.of(text(20), text(300), text(1000));

        assertThat(VALIDATOR.validate(command)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidCodes")
    @DisplayName("코드 규칙을 위반하면 code 위반만 남는다")
    void flagsCodeViolations(String code) {
        var violations = VALIDATOR.validate(CreateRoomCommand.of(code, NAME, DESCRIPTION));

        assertThat(violations)
                .hasSize(1)
                .allMatch(violation -> "code".equals(violation.getPropertyPath().toString()));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    @DisplayName("이름 규칙을 위반하면 name 위반만 남는다")
    void flagsNameViolations(String name) {
        var violations = VALIDATOR.validate(CreateRoomCommand.of(CODE, name, DESCRIPTION));

        assertThat(violations)
                .hasSize(1)
                .allMatch(violation -> "name".equals(violation.getPropertyPath().toString()));
    }

    @ParameterizedTest
    @MethodSource("invalidDescriptions")
    @DisplayName("설명 규칙을 위반하면 description 위반만 남는다")
    void flagsDescriptionViolations(String description) {
        var violations = VALIDATOR.validate(CreateRoomCommand.of(CODE, NAME, description));

        assertThat(violations)
                .hasSize(1)
                .allMatch(
                        violation -> "description".equals(violation.getPropertyPath().toString()));
    }

    @ParameterizedTest
    @MethodSource("inputCombinations")
    @DisplayName("커맨드 검증 통과 여부는 Room 생성자 규칙과 항상 같다")
    void validationMatchesRoomConstructorRules(String code, String name, String description) {
        var command = CreateRoomCommand.of(code, name, description);

        assertThat(VALIDATOR.validate(command).isEmpty())
                .isEqualTo(roomAccepts(code, name, description));
    }

    @Test
    @DisplayName("빈 설명은 커맨드와 Room 모두 통과한다")
    void acceptsBlankDescription() {
        var command = CreateRoomCommand.of(CODE, NAME, " ");

        assertThat(VALIDATOR.validate(command)).isEmpty();
        assertThat(roomAccepts(CODE, NAME, " ")).isTrue();
    }

    private static boolean roomAccepts(String code, String name, String description) {
        try {
            Room.of(ROOM_ID, code, name, description, CREATED_AT);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Stream<Arguments> inputCombinations() {
        List<String> codes = Arrays.asList(null, "", " ", CODE, text(20), text(21));
        List<String> names = Arrays.asList(null, "", " ", NAME, text(300), text(301));
        List<String> descriptions =
                Arrays.asList(null, "", " ", DESCRIPTION, text(1000), text(1001));

        return codes.stream()
                .flatMap(
                        code ->
                                names.stream()
                                        .flatMap(
                                                name ->
                                                        descriptions.stream()
                                                                .map(
                                                                        description ->
                                                                                arguments(
                                                                                        code,
                                                                                        name,
                                                                                        description))));
    }

    private static List<String> invalidCodes() {
        return Arrays.asList(null, "", " ", text(21));
    }

    private static List<String> invalidNames() {
        return Arrays.asList(null, "", " ", text(301));
    }

    private static List<String> invalidDescriptions() {
        return Arrays.asList(null, text(1001));
    }

    private static String text(int length) {
        return "a".repeat(length);
    }
}
