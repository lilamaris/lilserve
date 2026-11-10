package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import com.lilamaris.lilserve.reservation.application.service.UpdateSeatService;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
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
import org.junit.jupiter.api.Test;
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

@DisplayName("좌석 수정과 삭제 및 복구의 동시성")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({SeatJpaAdapter.class, UpdateSeatConcurrencyTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UpdateSeatConcurrencyTest {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(60);
    private static final Instant CREATED_AT = NOW.minusSeconds(3600);

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private SeatJpaAdapter adapter;

    @Autowired
    @Qualifier("firstService")
    private UpdateSeatService firstService;

    @Autowired
    @Qualifier("laterService")
    private UpdateSeatService laterService;

    @Autowired
    @Qualifier("failingService")
    private UpdateSeatService failingService;

    private final SeatId seatId = SeatId.of(UUID.randomUUID());
    private final RoomId roomId = RoomId.of(UUID.randomUUID());

    @BeforeEach
    void commitsRoomAndActiveSeat() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(
                                    Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
                            entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
                        });
    }

    @AfterEach
    void deletesThisTestsRows() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager
                                    .createNativeQuery("delete from seat where id = :id")
                                    .setParameter("id", seatId.getValue())
                                    .executeUpdate();
                            entityManager
                                    .createNativeQuery("delete from room where id = :id")
                                    .setParameter("id", roomId.getValue())
                                    .executeUpdate();
                        });
    }

    @Test
    @DisplayName("코드 수정이 먼저 확정되면 대기한 삭제는 변경된 코드와 삭제 시각을 함께 보존한다")
    void preservesCodeWhenDeletionFollowsModification() throws Exception {
        race(Change.CODE, Change.DELETE, false, false);

        assertThat(snapshot())
                .isEqualTo(
                        new UpdateSeatResult(seatId, roomId, "SEAT-02", CREATED_AT, LATER, LATER));
    }

    @Test
    @DisplayName("삭제가 먼저 확정되면 대기한 코드 수정을 거부하고 삭제 상태와 기존 코드를 보존한다")
    void rejectsModificationAfterDeletion() throws Exception {
        race(Change.DELETE, Change.CODE, false, true);

        assertThat(snapshot())
                .isEqualTo(new UpdateSeatResult(seatId, roomId, "SEAT-01", CREATED_AT, NOW, NOW));
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"DELETE", "RESTORE"})
    @DisplayName("중복 삭제와 복구의 후속 요청을 거부하고 최초 확정 시각을 보존한다")
    void rejectsDuplicateChangeAndPreservesFirstTimestamp(Change change) throws Exception {
        prepare(change);
        race(change, change, false, true);

        assertThat(snapshot())
                .isEqualTo(
                        new UpdateSeatResult(
                                seatId,
                                roomId,
                                "SEAT-01",
                                CREATED_AT,
                                NOW,
                                change == Change.DELETE ? NOW : null));
    }

    @ParameterizedTest
    @EnumSource(
            value = Change.class,
            names = {"DELETE", "RESTORE"})
    @DisplayName("삭제와 복구는 선행 요청이 확정한 상태를 기준으로 후속 요청을 허용한다")
    void appliesOppositeStateChangeAfterCommit(Change first) throws Exception {
        prepare(first);
        race(first, first == Change.DELETE ? Change.RESTORE : Change.DELETE, false, false);

        assertThat(snapshot())
                .isEqualTo(
                        new UpdateSeatResult(
                                seatId,
                                roomId,
                                "SEAT-01",
                                CREATED_AT,
                                LATER,
                                first == Change.DELETE ? null : LATER));
    }

    @Test
    @DisplayName("선행 삭제가 롤백되면 대기한 코드 수정은 활성 상태를 조회하여 성공한다")
    void modifiesSeatAfterDeletionRollsBack() throws Exception {
        race(Change.DELETE, Change.CODE, true, false);

        assertThat(snapshot())
                .isEqualTo(
                        new UpdateSeatResult(seatId, roomId, "SEAT-02", CREATED_AT, LATER, null));
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
                                    adapter.findById(seatId)
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
                                    holdFirstChange(
                                            first,
                                            rollback,
                                            firstPid,
                                            () -> {
                                                firstChanged.countDown();
                                                await(releaseFirst);
                                            }));
            await(firstChanged);
            var secondRequest =
                    executor.submit(
                            () ->
                                    runSecondChange(
                                            second,
                                            rejected,
                                            () -> {
                                                secondPid.set(backendPid());
                                                secondStarted.countDown();
                                            }));
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

    private void holdFirstChange(
            Change change, boolean rollback, AtomicInteger pid, Runnable afterChange) {
        transaction()
                .executeWithoutResult(
                        status -> {
                            pid.set(backendPid());
                            apply(firstService, change);
                            // Pause before flushing so an UPDATE lock cannot hide a missing
                            // exclusive read lock.
                            afterChange.run();
                            if (rollback) status.setRollbackOnly();
                        });
    }

    private void runSecondChange(Change change, boolean rejected, Runnable beforeChange) {
        Runnable request =
                () ->
                        transaction()
                                .executeWithoutResult(
                                        status -> {
                                            beforeChange.run();
                                            apply(laterService, change);
                                        });
        if (rejected) {
            assertThatThrownBy(request::run).isInstanceOf(NoSuchElementException.class);
        } else {
            request.run();
        }
    }

    private UpdateSeatResult apply(UpdateSeatService service, Change change) {
        return switch (change) {
            case CODE -> service.updateCode(UpdateSeatCodeCommand.of(seatId, "SEAT-02"));
            case DELETE -> service.delete(DeleteSeatCommand.of(seatId));
            case RESTORE -> service.restore(RestoreSeatCommand.of(seatId));
        };
    }

    private UpdateSeatResult snapshot() {
        return transaction()
                .execute(status -> UpdateSeatResult.from(adapter.findById(seatId).orElseThrow()));
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
        DELETE,
        RESTORE
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        UpdateSeatService firstService(SeatJpaAdapter adapter) {
            return new UpdateSeatService(adapter, adapter, Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        UpdateSeatService laterService(SeatJpaAdapter adapter) {
            return new UpdateSeatService(adapter, adapter, Clock.fixed(LATER, ZoneOffset.UTC));
        }

        @Bean
        UpdateSeatService failingService(SeatJpaAdapter adapter, EntityManager entityManager) {
            return new UpdateSeatService(
                    adapter,
                    seat -> {
                        adapter.save(seat);
                        entityManager.flush();
                        throw new IllegalStateException("Storage failed after flush");
                    },
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }
    }
}
