package com.lilamaris.lilserve.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.identity.domain.id.ExternalIdentityId;
import com.lilamaris.lilserve.identity.domain.id.UserId;
import com.lilamaris.lilserve.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilserve.kernel.exception.IllegalValueConditionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("외부 로그인 계정 연결")
class ExternalIdentityTest {
    private static final ExternalIdentityId ID = ExternalIdentityId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String PROVIDER = "example";
    private static final String ISSUER = "https://id.example.com";
    private static final String SUBJECT = "CaseSensitive-123";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("외부 인증 계정을 연결하면 식별 정보를 그대로 보관한다")
    void createsLinkedIdentity() {
        var identity = identity();

        assertThat(identity.getId()).isEqualTo(ID);
        assertThat(identity.getUserId()).isEqualTo(USER_ID);
        assertThat(identity.getProvider()).isEqualTo(PROVIDER);
        assertThat(identity.getIssuer()).isEqualTo(ISSUER);
        assertThat(identity.getSubject()).isEqualTo(SUBJECT);
        assertThat(identity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(identity.getUnlinkedAt()).isNull();
        assertThat(identity.isLinked()).isTrue();
    }

    @Test
    @DisplayName("연결을 해제하면 해제 시점을 기록하고 외부 식별자는 유지한다")
    void unlinksIdentity() {
        var identity = identity();
        var unlinkedAt = CREATED_AT.plusSeconds(60);

        identity.unlink(unlinkedAt);

        assertThat(identity.isLinked()).isFalse();
        assertThat(identity.getUnlinkedAt()).isEqualTo(unlinkedAt);
        assertThat(identity.getSubject()).isEqualTo(SUBJECT);
        assertThat(identity.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("이미 해제된 연결을 다시 해제할 수 없다")
    void rejectsRepeatedUnlink() {
        var identity = identity();
        identity.unlink(CREATED_AT);

        assertThatThrownBy(() -> identity.unlink(CREATED_AT.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(identity.getUnlinkedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("생성과 같은 시점에 연결을 해제할 수 있다")
    void acceptsEqualUnlinkTime() {
        var identity = identity();

        identity.unlink(CREATED_AT);

        assertThat(identity.getUnlinkedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("생성보다 이른 해제는 연결을 유지한다")
    void rejectsUnlinkBeforeCreation() {
        var identity = identity();

        assertThatThrownBy(() -> identity.unlink(CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(identity.isLinked()).isTrue();
        assertThat(identity.getUnlinkedAt()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("해제 시점이 없으면 연결을 유지한다")
    void rejectsNullUnlinkTime(Instant time) {
        var identity = identity();

        assertThatThrownBy(() -> identity.unlink(time)).isInstanceOf(NullPointerException.class);

        assertThat(identity.isLinked()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("공급자 식별자가 없으면 연결할 수 없다")
    void rejectsMissingProvider(String provider) {
        assertThatThrownBy(
                        () ->
                                ExternalIdentity.of(
                                        ID, USER_ID, provider, ISSUER, SUBJECT, CREATED_AT))
                .isInstanceOf(
                        provider == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("발급자 식별자가 없으면 연결할 수 없다")
    void rejectsMissingIssuer(String issuer) {
        assertThatThrownBy(
                        () ->
                                ExternalIdentity.of(
                                        ID, USER_ID, PROVIDER, issuer, SUBJECT, CREATED_AT))
                .isInstanceOf(
                        issuer == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("외부 사용자 식별자가 없으면 연결할 수 없다")
    void rejectsMissingSubject(String subject) {
        assertThatThrownBy(
                        () ->
                                ExternalIdentity.of(
                                        ID, USER_ID, PROVIDER, ISSUER, subject, CREATED_AT))
                .isInstanceOf(
                        subject == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("길이 상한을 넘는 외부 사용자 식별자를 거절한다")
    void rejectsLongSubject() {
        assertThatThrownBy(
                        () ->
                                ExternalIdentity.of(
                                        ID,
                                        USER_ID,
                                        PROVIDER,
                                        ISSUER,
                                        "s".repeat(ExternalIdentity.MAX_SUBJECT_LENGTH + 1),
                                        CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("외부 식별자의 길이 상한을 허용한다")
    void acceptsMaximumLengths() {
        var provider = "p".repeat(ExternalIdentity.MAX_PROVIDER_LENGTH);
        var issuer = "i".repeat(ExternalIdentity.MAX_ISSUER_LENGTH);
        var subject = "s".repeat(ExternalIdentity.MAX_SUBJECT_LENGTH);

        var identity = ExternalIdentity.of(ID, USER_ID, provider, issuer, subject, CREATED_AT);

        assertThat(identity.getProvider()).isEqualTo(provider);
        assertThat(identity.getIssuer()).isEqualTo(issuer);
        assertThat(identity.getSubject()).isEqualTo(subject);
    }

    @Test
    @DisplayName("연결 식별자가 없으면 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(
                        () ->
                                ExternalIdentity.of(
                                        null, USER_ID, PROVIDER, ISSUER, SUBJECT, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("소유 사용자가 없으면 생성할 수 없다")
    void rejectsNullUserId() {
        assertThatThrownBy(
                        () -> ExternalIdentity.of(ID, null, PROVIDER, ISSUER, SUBJECT, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("생성 시점이 없으면 연결할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> ExternalIdentity.of(ID, USER_ID, PROVIDER, ISSUER, SUBJECT, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static ExternalIdentity identity() {
        return ExternalIdentity.of(ID, USER_ID, PROVIDER, ISSUER, SUBJECT, CREATED_AT);
    }
}
