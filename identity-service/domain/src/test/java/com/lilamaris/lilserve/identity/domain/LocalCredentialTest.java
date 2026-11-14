package com.lilamaris.lilserve.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("이메일과 비밀번호 인증 정보")
class LocalCredentialTest {
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String EMAIL = "alice@example.com";
    private static final String HASH = "{test}encoded-password";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final Instant LATER_AT = UPDATED_AT.plusSeconds(60);

    @Test
    @DisplayName("생성하면 사용자와 이메일 및 해시를 보관하고 미인증 상태로 시작한다")
    void createsUnverifiedCredential() {
        var credential = credential();

        assertThat(credential.getUserId()).isEqualTo(USER_ID);
        assertThat(credential.getEmail()).isEqualTo(EMAIL);
        assertThat(credential.getPasswordHash()).isEqualTo(HASH);
        assertThat(credential.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(credential.getUpdatedAt()).isNull();
        assertThat(credential.getEmailVerifiedAt()).isNull();
        assertThat(credential.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("현재 이메일을 인증하면 인증과 갱신 시점을 기록한다")
    void verifiesCurrentEmail() {
        var credential = credential();

        credential.verifyEmail(EMAIL, UPDATED_AT);

        assertThat(credential.isEmailVerified()).isTrue();
        assertThat(credential.getEmailVerifiedAt()).isEqualTo(UPDATED_AT);
        assertThat(credential.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("이미 인증한 이메일을 다시 인증하면 기존 인증 시점을 유지한다")
    void rejectsRepeatedVerification() {
        var credential = credential();
        credential.verifyEmail(EMAIL, UPDATED_AT);

        assertThatThrownBy(() -> credential.verifyEmail(EMAIL, LATER_AT))
                .isInstanceOf(IllegalStateException.class);

        assertThat(credential.getEmailVerifiedAt()).isEqualTo(UPDATED_AT);
        assertThat(credential.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("다른 이메일로 변경하면 이전 이메일 인증 상태를 초기화한다")
    void clearsVerificationWhenEmailChanges() {
        var credential = credential();
        credential.verifyEmail(EMAIL, UPDATED_AT);

        credential.updateEmail("bob@example.com", LATER_AT);

        assertThat(credential.getEmail()).isEqualTo("bob@example.com");
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("이메일 변경 전 주소의 인증 결과로 새 주소를 인증할 수 없다")
    void rejectsVerificationForPreviousEmail() {
        var credential = credential();
        credential.updateEmail("bob@example.com", UPDATED_AT);

        assertThatThrownBy(() -> credential.verifyEmail(EMAIL, LATER_AT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(credential.getEmail()).isEqualTo("bob@example.com");
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("같은 이메일로 갱신하면 인증 상태를 유지한다")
    void preservesVerificationForSameEmail() {
        var credential = credential();
        credential.verifyEmail(EMAIL, UPDATED_AT);

        credential.updateEmail(EMAIL, LATER_AT);

        assertThat(credential.getEmailVerifiedAt()).isEqualTo(UPDATED_AT);
        assertThat(credential.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("비밀번호 해시 변경은 이메일 인증 상태를 유지한다")
    void replacesPasswordHash() {
        var credential = credential();
        credential.verifyEmail(EMAIL, UPDATED_AT);

        credential.updatePasswordHash("{test}replacement", LATER_AT);

        assertThat(credential.getPasswordHash()).isEqualTo("{test}replacement");
        assertThat(credential.getEmailVerifiedAt()).isEqualTo(UPDATED_AT);
        assertThat(credential.getUpdatedAt()).isEqualTo(LATER_AT);
        assertThat(credential.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("비어 있는 이메일로 생성할 수 없다")
    void rejectsBlankEmail(String email) {
        assertThatThrownBy(() -> LocalCredential.of(USER_ID, email, HASH, CREATED_AT))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("비어 있는 비밀번호 해시로 생성할 수 없다")
    void rejectsBlankHash(String hash) {
        assertThatThrownBy(() -> LocalCredential.of(USER_ID, EMAIL, hash, CREATED_AT))
                .isInstanceOf(IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("필수 사용자 식별자가 없으면 생성할 수 없다")
    void rejectsNullUserId() {
        assertThatThrownBy(() -> LocalCredential.of(null, EMAIL, HASH, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("필수 생성 시점이 없으면 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> LocalCredential.of(USER_ID, EMAIL, HASH, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("이메일과 해시의 길이 상한은 허용한다")
    void acceptsMaximumLengths() {
        var email = "a".repeat(LocalCredential.MAX_EMAIL_LENGTH);
        var hash = "h".repeat(LocalCredential.MAX_PASSWORD_HASH_LENGTH);

        var credential = LocalCredential.of(USER_ID, email, hash, CREATED_AT);

        assertThat(credential.getEmail()).isEqualTo(email);
        assertThat(credential.getPasswordHash()).isEqualTo(hash);
    }

    @Test
    @DisplayName("길이 상한을 넘는 이메일 변경은 인증 상태와 시점을 유지한다")
    void rejectsLongEmailUpdate() {
        var credential = credential();
        credential.verifyEmail(EMAIL, UPDATED_AT);

        assertThatThrownBy(
                        () ->
                                credential.updateEmail(
                                        "a".repeat(LocalCredential.MAX_EMAIL_LENGTH + 1), LATER_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(credential.getEmail()).isEqualTo(EMAIL);
        assertThat(credential.getEmailVerifiedAt()).isEqualTo(UPDATED_AT);
        assertThat(credential.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("길이 상한을 넘는 해시 변경은 기존 해시와 시점을 유지한다")
    void rejectsLongHashUpdate() {
        var credential = credential();

        assertThatThrownBy(
                        () ->
                                credential.updatePasswordHash(
                                        "h".repeat(LocalCredential.MAX_PASSWORD_HASH_LENGTH + 1),
                                        UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(credential.getPasswordHash()).isEqualTo(HASH);
        assertThat(credential.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @MethodSource("mutations")
    @DisplayName("생성보다 이른 변경은 거절하고 인증 정보를 유지한다")
    void rejectsTimeBeforeCreation(BiConsumer<LocalCredential, Instant> mutation) {
        var credential = credential();

        assertThatThrownBy(() -> mutation.accept(credential, CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertUnchanged(credential);
    }

    @ParameterizedTest
    @MethodSource("mutations")
    @DisplayName("마지막 갱신보다 이른 변경은 거절하고 인증 정보를 유지한다")
    void rejectsTimeBeforeLastUpdate(BiConsumer<LocalCredential, Instant> mutation) {
        var credential = credential();
        credential.updatePasswordHash(HASH, UPDATED_AT);

        assertThatThrownBy(() -> mutation.accept(credential, CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(credential.getEmail()).isEqualTo(EMAIL);
        assertThat(credential.getPasswordHash()).isEqualTo(HASH);
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @MethodSource("mutations")
    @DisplayName("변경 시점이 없으면 인증 정보를 유지한다")
    void rejectsNullTime(BiConsumer<LocalCredential, Instant> mutation) {
        var credential = credential();

        assertThatThrownBy(() -> mutation.accept(credential, null))
                .isInstanceOf(NullPointerException.class);

        assertUnchanged(credential);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("누락된 인증 주소로 이메일을 인증할 수 없다")
    void rejectsMissingVerificationEmail(String email) {
        var credential = credential();

        assertThatThrownBy(() -> credential.verifyEmail(email, UPDATED_AT))
                .isInstanceOf(
                        email == null
                                ? NullPointerException.class
                                : IllegalArgumentException.class);

        assertUnchanged(credential);
    }

    @Test
    @DisplayName("생성과 같은 시점에 인증할 수 있다")
    void acceptsEqualTime() {
        var credential = credential();

        credential.verifyEmail(EMAIL, CREATED_AT);

        assertThat(credential.getEmailVerifiedAt()).isEqualTo(CREATED_AT);
    }

    private static void assertUnchanged(LocalCredential credential) {
        assertThat(credential.getEmail()).isEqualTo(EMAIL);
        assertThat(credential.getPasswordHash()).isEqualTo(HASH);
        assertThat(credential.isEmailVerified()).isFalse();
        assertThat(credential.getUpdatedAt()).isNull();
    }

    private static Stream<BiConsumer<LocalCredential, Instant>> mutations() {
        return Stream.of(
                (credential, time) -> credential.updateEmail("bob@example.com", time),
                (credential, time) -> credential.updatePasswordHash("{test}replacement", time),
                (credential, time) -> credential.verifyEmail(EMAIL, time));
    }

    private static LocalCredential credential() {
        return LocalCredential.of(USER_ID, EMAIL, HASH, CREATED_AT);
    }
}
