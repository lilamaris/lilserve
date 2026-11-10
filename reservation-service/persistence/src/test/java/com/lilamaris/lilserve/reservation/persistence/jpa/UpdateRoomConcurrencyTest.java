package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import com.lilamaris.lilserve.reservation.application.service.UpdateRoomService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("룸 수정과 삭제 및 복구의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({RoomJpaAdapter.class, UpdateRoomConcurrencyTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UpdateRoomConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private RoomJpaAdapter adapter;

    @Autowired
    @Qualifier("firstService")
    private UpdateRoomService firstService;

    @Autowired
    @Qualifier("laterService")
    private UpdateRoomService laterService;

    @Autowired
    @Qualifier("failingService")
    private UpdateRoomService failingService;

    private final RoomId roomId = RoomId.of(UUID.randomUUID());

    @BeforeEach
    void commitsActiveRoom() {
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager.persist(
                                        Room.of(
                                                roomId,
                                                "ROOM-01",
                                                "hall",
                                                "original",
                                                CREATED_AT)));
    }

    @AfterEach
    void deletesThisTestsRoom() {
        transaction()
                .executeWithoutResult(
                        status ->
                                entityManager
                                        .createNativeQuery("delete from room where id = :id")
                                        .setParameter("id", roomId.getValue())
                                        .executeUpdate());
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"CODE", "METADATA"})
    @DisplayName("코드와 메타데이터 변경은 배타 락 순서로 처리되어 두 변경 값을 보존한다")
    void preservesBothChanges(Change first) throws Exception {
        race(first, first == Change.CODE ? Change.METADATA : Change.CODE, false, false);

        var saved = snapshot();
        assertThat(saved.code()).isEqualTo("ROOM-02");
        assertThat(saved.name()).isEqualTo("new hall");
        assertThat(saved.description()).isEqualTo("new description");
        assertThat(saved.updatedAt()).isEqualTo(LATER);
        assertThat(saved.deletedAt()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"CODE", "METADATA"})
    @DisplayName("삭제가 먼저 확정되면 대기한 수정을 거부하고 삭제 상태와 기존 값을 보존한다")
    void rejectsModificationAfterDeletion(Change modification) throws Exception {
        race(Change.DELETE, modification, false, true);

        var saved = snapshot();
        assertThat(saved.code()).isEqualTo("ROOM-01");
        assertThat(saved.name()).isEqualTo("hall");
        assertThat(saved.description()).isEqualTo("original");
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(saved.deletedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"DELETE", "RESTORE"})
    @DisplayName("중복 삭제와 복구의 후속 요청을 거부하고 최초 확정 시각을 보존한다")
    void rejectsDuplicateChangeAndPreservesFirstTimestamp(Change change) throws Exception {
        prepare(change);
        race(change, change, false, true);

        var saved = snapshot();
        assertThat(saved.updatedAt()).isEqualTo(NOW);
        assertThat(saved.deletedAt()).isEqualTo(change == Change.DELETE ? NOW : null);
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"DELETE", "RESTORE"})
    @DisplayName("삭제와 복구는 선행 요청이 확정한 상태를 기준으로 후속 요청을 허용한다")
    void appliesOppositeStateChangeAfterCommit(Change first) throws Exception {
        prepare(first);
        race(first, first == Change.DELETE ? Change.RESTORE : Change.DELETE, false, false);

        var saved = snapshot();
        assertThat(saved.updatedAt()).isEqualTo(LATER);
        assertThat(saved.deletedAt()).isEqualTo(first == Change.DELETE ? null : LATER);
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"CODE", "METADATA"})
    @DisplayName("선행 삭제가 롤백되면 대기한 수정은 활성 상태를 조회하여 성공한다")
    void modifiesRoomAfterDeletionRollsBack(Change modification) throws Exception {
        race(Change.DELETE, modification, true, false);

        var saved = snapshot();
        assertThat(saved.deletedAt()).isNull();
        assertThat(saved.updatedAt()).isEqualTo(LATER);
        assertThat(saved.code()).isEqualTo(modification == Change.CODE ? "ROOM-02" : "ROOM-01");
        assertThat(saved.name()).isEqualTo(modification == Change.METADATA ? "new hall" : "hall");
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("저장 후 예외가 발생하면 서비스 트랜잭션이 플러시한 변경까지 롤백한다")
    void rollsBackFlushedChangesOnFailure(Change change) {
        prepare(change);
        var before = snapshot();

        assertThatThrownBy(() -> apply(failingService, change))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");

        assertThat(snapshot()).isEqualTo(before);
    }

    private void prepare(Change change) {
        if (change == Change.RESTORE) {
            transaction()
                    .executeWithoutResult(
                            status ->
                                    adapter.findById(roomId)
                                            .orElseThrow()
                                            .delete(NOW.minusSeconds(60)));
        }
    }

    private void race(Change first, Change second, boolean rollback, boolean rejected)
            throws Exception {
        var firstChanged = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var firstPid = new AtomicInteger();
        var secondPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstRequest =
                    executor.submit(
                            () ->
                                    transaction()
                                            .executeWithoutResult(
                                                    status -> {
                                                        firstPid.set(backendPid());
                                                        apply(firstService, first);
                                                        // Hold the lock before flushing: without
                                                        // the read lock the second request could
                                                        // read the old state and commit, so this
                                                        // boundary detects missing serialization.
                                                        firstChanged.countDown();
                                                        await(releaseFirst);
                                                        if (rollback) status.setRollbackOnly();
                                                    }));
            await(firstChanged);
            var secondRequest =
                    executor.submit(
                            () -> {
                                Runnable request =
                                        () ->
                                                transaction()
                                                        .executeWithoutResult(
                                                                status -> {
                                                                    secondPid.set(backendPid());
                                                                    secondStarted.countDown();
                                                                    apply(laterService, second);
                                                                });
                                if (rejected) {
                                    assertThatThrownBy(request::run)
                                            .isInstanceOf(NoSuchElementException.class);
                                } else {
                                    request.run();
                                }
                            });
            await(secondStarted);
            awaitBlockedBy(secondPid.get(), firstPid.get());
            releaseFirst.countDown();
            firstRequest.get(20, TimeUnit.SECONDS);
            secondRequest.get(20, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private UpdateRoomResult apply(UpdateRoomService service, Change change) {
        return switch (change) {
            case CODE -> service.updateCode(UpdateRoomCodeCommand.of(roomId, "ROOM-02"));
            case METADATA ->
                    service.updateMetadata(
                            UpdateRoomMetadataCommand.of(roomId, "new hall", "new description"));
            case DELETE -> service.delete(DeleteRoomCommand.of(roomId));
            case RESTORE -> service.restore(RestoreRoomCommand.of(roomId));
        };
    }

    private UpdateRoomResult snapshot() {
        return transaction()
                .execute(status -> UpdateRoomResult.from(adapter.findById(roomId).orElseThrow()));
    }

    private int backendPid() {
        return ((Number)
                        entityManager
                                .createNativeQuery("select pg_backend_pid()")
                                .getSingleResult())
                .intValue();
    }

    private void awaitBlockedBy(int blockedPid, int blockerPid) {
        var deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        do {
            var blocked =
                    transaction()
                            .execute(
                                    status ->
                                            entityManager
                                                    .createNativeQuery(
                                                            "select :blocker = any(pg_blocking_pids(:blocked))",
                                                            Boolean.class)
                                                    .setParameter("blocker", blockerPid)
                                                    .setParameter("blocked", blockedPid)
                                                    .getSingleResult());
            if (Boolean.TRUE.equals(blocked)) return;
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        } while (System.nanoTime() < deadline && !Thread.currentThread().isInterrupted());
        throw new AssertionError("Expected backend " + blockedPid + " to wait for " + blockerPid);
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(
                    "Interrupted while waiting for transaction boundary", exception);
        }
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private enum Change {
        CODE,
        METADATA,
        DELETE,
        RESTORE
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        UpdateRoomService firstService(RoomJpaAdapter adapter) {
            return new UpdateRoomService(adapter, adapter, Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        UpdateRoomService laterService(RoomJpaAdapter adapter) {
            return new UpdateRoomService(adapter, adapter, Clock.fixed(LATER, ZoneOffset.UTC));
        }

        @Bean
        UpdateRoomService failingService(RoomJpaAdapter adapter, EntityManager entityManager) {
            return new UpdateRoomService(
                    adapter,
                    room -> {
                        adapter.save(room);
                        entityManager.flush();
                        throw new IllegalStateException("Storage failed after flush");
                    },
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
