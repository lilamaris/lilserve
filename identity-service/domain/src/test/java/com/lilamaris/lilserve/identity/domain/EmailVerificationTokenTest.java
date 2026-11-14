package com.lilamaris.lilserve.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.identity.domain.id.EmailVerificationTokenId;
import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("이메일 인증 토큰")
class EmailVerificationTokenTest {
    private static final EmailVerificationTokenId ID =
            EmailVerificationTokenId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String EMAIL = "alice@example.com";
    private static final String HASH = "test-token-digest";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = CREATED_AT.plusSeconds(600);
    private static final Instant USED_AT = CREATED_AT.plusSeconds(60);

    @Test
    @DisplayName("발급하면 사용자와 이메일 및 해시를 보관하고 사용과 폐기 시점을 비운다")
    void createsPendingToken() {
        var token = token();

        assertThat(token.getId()).isEqualTo(ID);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getEmail()).isEqualTo(EMAIL);
        assertThat(token.getTokenHash()).isEqualTo(HASH);
        assertThat(token.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(token.getConsumedAt()).isNull();
        assertThat(token.getRevokedAt()).isNull();
    }

    @ParameterizedTest
    @MethodSource("missingRequiredValues")
    @DisplayName("필수 값이 없으면 토큰을 발급할 수 없다")
    void rejectsMissingRequiredValue(String field, Executable creation) {
        assertThatThrownBy(creation::execute).as(field).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("이메일이 비어 있으면 토큰을 발급할 수 없다")
    void rejectsBlankEmail(String email) {
        assertThatThrownBy(
                        () ->
                                EmailVerificationToken.of(
                                        ID, USER_ID, email, HASH, CREATED_AT, EXPIRES_AT))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("해시가 비어 있으면 토큰을 발급할 수 없다")
    void rejectsBlankHash(String hash) {
        assertThatThrownBy(
                        () ->
                                EmailVerificationToken.of(
                                        ID, USER_ID, EMAIL, hash, CREATED_AT, EXPIRES_AT))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("이메일과 해시 길이가 상한과 같으면 발급할 수 있다")
    void acceptsMaximumLengths() {
        var email = "a".repeat(LocalCredential.MAX_EMAIL_LENGTH);
        var hash = "h".repeat(EmailVerificationToken.MAX_TOKEN_HASH_LENGTH);

        var token = EmailVerificationToken.of(ID, USER_ID, email, hash, CREATED_AT, EXPIRES_AT);

        assertThat(token.getEmail()).isEqualTo(email);
        assertThat(token.getTokenHash()).isEqualTo(hash);
    }

    @Test
    @DisplayName("길이 상한을 넘는 이메일은 거절한다")
    void rejectsLongEmail() {
        assertThatThrownBy(
                        () ->
                                EmailVerificationToken.of(
                                        ID,
                                        USER_ID,
                                        "a".repeat(LocalCredential.MAX_EMAIL_LENGTH + 1),
                                        HASH,
                                        CREATED_AT,
                                        EXPIRES_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("길이 상한을 넘는 해시는 거절한다")
    void rejectsLongHash() {
        assertThatThrownBy(
                        () ->
                                EmailVerificationToken.of(
                                        ID,
                                        USER_ID,
                                        EMAIL,
                                        "h"
                                                .repeat(
                                                        EmailVerificationToken.MAX_TOKEN_HASH_LENGTH
                                                                + 1),
                                        CREATED_AT,
                                        EXPIRES_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    @DisplayName("만료 시점이 발급 시점보다 늦지 않으면 거절한다")
    void rejectsNonPositiveLifetime(long offset) {
        assertThatThrownBy(
                        () ->
                                EmailVerificationToken.of(
                                        ID,
                                        USER_ID,
                                        EMAIL,
                                        HASH,
                                        CREATED_AT,
                                        CREATED_AT.plusSeconds(offset)))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("만료 전 사용하면 사용 시점을 기록하고 발급 정보를 유지한다")
    void consumesToken() {
        var token = token();

        token.consume(USED_AT);

        assertThat(token.getConsumedAt()).isEqualTo(USED_AT);
        assertThat(token.getRevokedAt()).isNull();
        assertThat(token.getEmail()).isEqualTo(EMAIL);
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("발급과 같은 시점에 사용할 수 있다")
    void consumesAtIssuance() {
        var token = token();

        token.consume(CREATED_AT);

        assertThat(token.getConsumedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    @DisplayName("만료 시점부터 사용할 수 없고 사용 상태를 유지한다")
    void rejectsConsumptionAtOrAfterExpiry(long offset) {
        var token = token();

        assertThatThrownBy(() -> token.consume(EXPIRES_AT.plusSeconds(offset)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertPending(token);
    }

    @ParameterizedTest
    @MethodSource("transitions")
    @DisplayName("사용한 토큰은 재사용하거나 폐기할 수 없다")
    void rejectsTransitionsAfterConsumption(
            BiConsumer<EmailVerificationToken, Instant> transition) {
        var token = token();
        token.consume(USED_AT);

        assertThatThrownBy(() -> transition.accept(token, USED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(token.getConsumedAt()).isEqualTo(USED_AT);
        assertThat(token.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("폐기하면 폐기 시점을 기록하고 사용 시점을 비워 둔다")
    void revokesToken() {
        var token = token();

        token.revoke(USED_AT);

        assertThat(token.getRevokedAt()).isEqualTo(USED_AT);
        assertThat(token.getConsumedAt()).isNull();
    }

    @Test
    @DisplayName("만료된 미사용 토큰도 폐기할 수 있다")
    void revokesExpiredToken() {
        var token = token();

        token.revoke(EXPIRES_AT);

        assertThat(token.getRevokedAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("발급과 같은 시점에 폐기할 수 있다")
    void revokesAtIssuance() {
        var token = token();

        token.revoke(CREATED_AT);

        assertThat(token.getRevokedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @MethodSource("transitions")
    @DisplayName("폐기한 토큰은 사용하거나 다시 폐기할 수 없다")
    void rejectsTransitionsAfterRevocation(BiConsumer<EmailVerificationToken, Instant> transition) {
        var token = token();
        token.revoke(USED_AT);

        assertThatThrownBy(() -> transition.accept(token, USED_AT.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(token.getRevokedAt()).isEqualTo(USED_AT);
        assertThat(token.getConsumedAt()).isNull();
    }

    @ParameterizedTest
    @MethodSource("transitions")
    @DisplayName("발급보다 이른 상태 변경은 거절하고 기존 상태를 유지한다")
    void rejectsTimeBeforeIssuance(BiConsumer<EmailVerificationToken, Instant> transition) {
        var token = token();

        assertThatThrownBy(() -> transition.accept(token, CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertPending(token);
    }

    @ParameterizedTest
    @MethodSource("transitions")
    @DisplayName("상태 변경 시점이 없으면 거절하고 기존 상태를 유지한다")
    void rejectsNullTransitionTime(BiConsumer<EmailVerificationToken, Instant> transition) {
        var token = token();

        assertThatThrownBy(() -> transition.accept(token, null))
                .isInstanceOf(NullPointerException.class);

        assertPending(token);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    @DisplayName("만료 시점부터 만료된 것으로 판단한다")
    void evaluatesExpiryBoundary(long offset) {
        assertThat(token().isExpired(EXPIRES_AT.plusSeconds(offset))).isEqualTo(offset >= 0);
    }

    @Test
    @DisplayName("만료 판단 시점이 없으면 거절한다")
    void rejectsNullExpiryTime() {
        assertThatThrownBy(() -> token().isExpired(null)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> missingRequiredValues() {
        return Stream.of(
                Arguments.of(
                        "id",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                null,
                                                USER_ID,
                                                EMAIL,
                                                HASH,
                                                CREATED_AT,
                                                EXPIRES_AT)),
                Arguments.of(
                        "userId",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                ID, null, EMAIL, HASH, CREATED_AT, EXPIRES_AT)),
                Arguments.of(
                        "email",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                ID, USER_ID, null, HASH, CREATED_AT, EXPIRES_AT)),
                Arguments.of(
                        "tokenHash",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                ID, USER_ID, EMAIL, null, CREATED_AT, EXPIRES_AT)),
                Arguments.of(
                        "createdAt",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                ID, USER_ID, EMAIL, HASH, null, EXPIRES_AT)),
                Arguments.of(
                        "expiresAt",
                        (Executable)
                                () ->
                                        EmailVerificationToken.of(
                                                ID, USER_ID, EMAIL, HASH, CREATED_AT, null)));
    }

    private static Stream<BiConsumer<EmailVerificationToken, Instant>> transitions() {
        return Stream.of(EmailVerificationToken::consume, EmailVerificationToken::revoke);
    }

    private static void assertPending(EmailVerificationToken token) {
        assertThat(token.getConsumedAt()).isNull();
        assertThat(token.getRevokedAt()).isNull();
    }

    private static EmailVerificationToken token() {
        return EmailVerificationToken.of(ID, USER_ID, EMAIL, HASH, CREATED_AT, EXPIRES_AT);
    }
}
