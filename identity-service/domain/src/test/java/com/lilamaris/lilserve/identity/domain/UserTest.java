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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("사용자")
class UserTest {
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(60);
    private static final Instant LATER_AT = UPDATED_AT.plusSeconds(60);

    @Test
    @DisplayName("사용자를 생성하면 활성 상태와 입력값을 보관하고 갱신과 삭제 시점을 비운다")
    void createsActiveUser() {
        var user = user();

        assertThat(user.getId()).isEqualTo(USER_ID);
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(user.getUpdatedAt()).isNull();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("표시 이름이 없거나 공백이면 사용자를 생성할 수 없다")
    void rejectsMissingDisplayName(String name) {
        assertThatThrownBy(() -> User.of(USER_ID, name, CREATED_AT))
                .isInstanceOf(
                        name == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);
    }

    @Test
    @DisplayName("필수 식별자가 없으면 사용자를 생성할 수 없다")
    void rejectsNullId() {
        assertThatThrownBy(() -> User.of(null, "Alice", CREATED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("생성 시점이 없으면 사용자를 생성할 수 없다")
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> User.of(USER_ID, "Alice", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("표시 이름 길이가 상한과 같으면 생성할 수 있다")
    void acceptsMaximumDisplayName() {
        var name = "a".repeat(User.MAX_DISPLAY_NAME_LENGTH);

        assertThat(User.of(USER_ID, name, CREATED_AT).getDisplayName()).isEqualTo(name);
    }

    @Test
    @DisplayName("표시 이름 길이가 상한을 넘으면 생성할 수 없다")
    void rejectsLongDisplayName() {
        var name = "a".repeat(User.MAX_DISPLAY_NAME_LENGTH + 1);

        assertThatThrownBy(() -> User.of(USER_ID, name, CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);
    }

    @Test
    @DisplayName("표시 이름을 변경하면 새 이름과 갱신 시점을 보관한다")
    void updatesDisplayName() {
        var user = user();

        user.updateDisplayName("Bob", UPDATED_AT);

        assertThat(user.getDisplayName()).isEqualTo("Bob");
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(user.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("유효하지 않은 표시 이름으로 변경하면 기존 이름과 시점을 유지한다")
    void preservesStateOnInvalidDisplayName(String name) {
        var user = user();

        assertThatThrownBy(() -> user.updateDisplayName(name, UPDATED_AT))
                .isInstanceOf(
                        name == null
                                ? NullPointerException.class
                                : IllegalValueConditionException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("상한을 넘는 이름으로 변경하면 기존 이름과 시점을 유지한다")
    void preservesStateOnLongDisplayName() {
        var user = user();

        assertThatThrownBy(
                        () ->
                                user.updateDisplayName(
                                        "a".repeat(User.MAX_DISPLAY_NAME_LENGTH + 1), UPDATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("계정을 정지하면 인증할 수 없고 정지 시점을 기록한다")
    void suspendsUser() {
        var user = user();

        user.suspend(UPDATED_AT);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("정지된 계정의 표시 이름을 변경할 수 있다")
    void renamesSuspendedUser() {
        var user = user();
        user.suspend(UPDATED_AT);

        user.updateDisplayName("Bob", LATER_AT);

        assertThat(user.getDisplayName()).isEqualTo("Bob");
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("정지를 해제하면 활성 상태와 해제 시점을 기록한다")
    void resumesSuspendedUser() {
        var user = user();
        user.suspend(UPDATED_AT);

        user.resume(LATER_AT);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("이미 정지된 계정은 다시 정지할 수 없다")
    void rejectsRepeatedSuspension() {
        var user = user();
        user.suspend(UPDATED_AT);

        assertThatThrownBy(() -> user.suspend(LATER_AT)).isInstanceOf(IllegalStateException.class);

        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("활성 계정의 정지 해제를 거절한다")
    void rejectsResumeWhenActive() {
        var user = user();

        assertThatThrownBy(() -> user.resume(UPDATED_AT)).isInstanceOf(IllegalStateException.class);

        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("사용자를 삭제하면 인증할 수 없고 삭제와 갱신 시점을 기록한다")
    void deletesUser() {
        var user = user();

        user.delete(UPDATED_AT);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("삭제된 활성 사용자를 복구하면 다시 활성화한다")
    void restoresActiveUser() {
        var user = user();
        user.delete(UPDATED_AT);

        user.restore(LATER_AT);

        assertThat(user.isActive()).isTrue();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(LATER_AT);
    }

    @Test
    @DisplayName("정지된 사용자는 삭제 후 복구하더라도 정지를 유지한다")
    void preservesSuspensionOnRestore() {
        var user = user();
        user.suspend(CREATED_AT);
        user.delete(UPDATED_AT);

        user.restore(LATER_AT);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.isActive()).isFalse();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제되지 않은 사용자를 복구할 수 없다")
    void rejectsRestoreWhenNotDeleted() {
        var user = user();

        assertThatThrownBy(() -> user.restore(UPDATED_AT))
                .isInstanceOf(IllegalStateException.class);

        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @ParameterizedTest
    @MethodSource("mutations")
    @DisplayName("삭제된 사용자의 변경은 거절하고 기존 값을 유지한다")
    void rejectsMutationsWhenDeleted(BiConsumer<User, Instant> mutation) {
        var user = user();
        user.delete(UPDATED_AT);

        assertThatThrownBy(() -> mutation.accept(user, LATER_AT))
                .isInstanceOf(IllegalStateException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @MethodSource("timeMutations")
    @DisplayName("마지막 변경보다 이른 변경 요청은 거절하고 기존 값을 유지한다")
    void rejectsTimeBeforeLastUpdate(BiConsumer<User, Instant> mutation) {
        var user = user();
        user.updateDisplayName("Alice", UPDATED_AT);

        assertThatThrownBy(() -> mutation.accept(user, CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("생성보다 이른 이름 변경은 거절하고 이름을 유지한다")
    void rejectsTimeBeforeCreation() {
        var user = user();

        assertThatThrownBy(() -> user.updateDisplayName("Bob", CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("변경 시점이 없으면 이름과 갱신 시점을 유지한다")
    void rejectsNullUpdateTime() {
        var user = user();

        assertThatThrownBy(() -> user.updateDisplayName("Bob", null))
                .isInstanceOf(NullPointerException.class);

        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("삭제보다 이른 시점의 복구는 거절하고 삭제 상태를 유지한다")
    void rejectsRestoreBeforeDeletion() {
        var user = user();
        user.delete(UPDATED_AT);

        assertThatThrownBy(() -> user.restore(CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(UPDATED_AT);
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("생성과 같은 시점에 정지하고 해제할 수 있다")
    void acceptsEqualTimestamp() {
        var user = user();
        user.suspend(CREATED_AT);

        user.resume(CREATED_AT);

        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("정지보다 이른 시점의 해제는 거절하고 정지를 유지한다")
    void rejectsResumeBeforeSuspension() {
        var user = user();
        user.suspend(UPDATED_AT);

        assertThatThrownBy(() -> user.resume(CREATED_AT))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    private static Stream<BiConsumer<User, Instant>> mutations() {
        return Stream.of(
                (user, time) -> user.updateDisplayName("Bob", time),
                User::suspend,
                User::resume,
                User::delete);
    }

    private static Stream<BiConsumer<User, Instant>> timeMutations() {
        return Stream.of(
                (user, time) -> user.updateDisplayName("Bob", time), User::suspend, User::delete);
    }

    private static User user() {
        return User.of(USER_ID, "Alice", CREATED_AT);
    }
}
