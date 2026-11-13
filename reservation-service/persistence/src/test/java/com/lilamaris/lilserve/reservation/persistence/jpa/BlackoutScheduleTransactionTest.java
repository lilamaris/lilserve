package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.service.CreateBlackoutScheduleService;
import com.lilamaris.lilserve.reservation.application.service.UpdateBlackoutScheduleService;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies blackout use case commit and rollback boundaries and exclusive lock lifetime against
 * PostgreSQL.
 */
@DisplayName("차단 스케줄 유스케이스 트랜잭션")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
    BlackoutScheduleJpaAdapter.class,
    RoomJpaAdapter.class,
    ScheduleJpaAdapter.class,
    BlackoutScheduleTransactionTest.Configuration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BlackoutScheduleTransactionTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(60);
    private static final Instant START = CREATED.plusSeconds(3600);
    private static final Instant END = CREATED.plusSeconds(7200);
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private BlackoutScheduleJpaAdapter adapter;

    @Autowired
    @Qualifier("createService")
    private CreateBlackoutScheduleService createService;

    @Autowired
    @Qualifier("updateService")
    private UpdateBlackoutScheduleService updateService;

    @Autowired
    @Qualifier("failingUpdateService")
    private UpdateBlackoutScheduleService failingUpdateService;

    @Autowired
    @Qualifier("failingCreateService")
    private CreateBlackoutScheduleService failingCreateService;

    private RoomId roomId;
    private ScheduleId scheduleId;
    private BlackoutScheduleId id;

    @BeforeEach
    void setUp() {
        roomId = RoomId.of(UUID.randomUUID());
        scheduleId = ScheduleId.of(UUID.randomUUID());
        id = BlackoutScheduleId.of(UUID.randomUUID());
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager.persist(Room.of(roomId, "ROOM-01", "hall", "", CREATED));
                            entityManager.persist(
                                    Schedule.of(
                                            scheduleId,
                                            roomId,
                                            DayOfWeek.MONDAY,
                                            LocalTime.of(10, 0),
                                            LocalTime.of(11, 0),
                                            CREATED));
                            entityManager.persist(
                                    BlackoutSchedule.of(
                                            id,
                                            roomId,
                                            scheduleId,
                                            "Maintenance",
                                            START,
                                            END,
                                            CREATED));
                        });
    }

    @AfterEach
    void cleanUp() {
        transaction()
                .executeWithoutResult(
                        status -> {
                            entityManager
                                    .createQuery(
                                            "DELETE FROM BlackoutSchedule b WHERE b.roomId = :id")
                                    .setParameter("id", roomId)
                                    .executeUpdate();
                            entityManager
                                    .createQuery("DELETE FROM Schedule s WHERE s.roomId = :id")
                                    .setParameter("id", roomId)
                                    .executeUpdate();
                            entityManager
                                    .createQuery("DELETE FROM Room r WHERE r.id = :id")
                                    .setParameter("id", roomId)
                                    .executeUpdate();
                        });
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("외부 트랜잭션 없이 호출해도 락 조회와 변경을 커밋한다")
    void commitsUseCaseWithoutCallerTransaction(Change change) {
        change(change, false);
        transaction()
                .executeWithoutResult(
                        status -> {
                            if (change == Change.CREATE) {
                                var created =
                                        entityManager
                                                .createQuery(
                                                        "SELECT b FROM BlackoutSchedule b WHERE b.roomId = :room AND b.id <> :id",
                                                        BlackoutSchedule.class)
                                                .setParameter("room", roomId)
                                                .setParameter("id", id)
                                                .getSingleResult();
                                assertThat(created.getCreatedAt()).isEqualTo(NOW);
                                assertThat(created.getStartsAt()).isEqualTo(START.plusSeconds(60));
                                assertThat(created.getEndsAt()).isEqualTo(END.plusSeconds(60));
                                assertThat(created.getReason()).isEqualTo("Inspection");
                                return;
                            }
                            var stored = entityManager.find(BlackoutSchedule.class, id);
                            assertThat(stored.getUpdatedAt()).isEqualTo(NOW);
                            switch (change) {
                                case REASON ->
                                        assertThat(stored.getReason()).isEqualTo("Inspection");
                                case PERIOD -> {
                                    assertThat(stored.getStartsAt())
                                            .isEqualTo(START.plusSeconds(60));
                                    assertThat(stored.getEndsAt()).isEqualTo(END.plusSeconds(60));
                                }
                                case DELETE -> assertThat(stored.getDeletedAt()).isEqualTo(NOW);
                                default -> throw new AssertionError("Unexpected change");
                            }
                        });
    }

    @ParameterizedTest
    @EnumSource(Change.class)
    @DisplayName("저장 후 플러시가 끝나도 실패하면 신규 생성과 기존 변경을 롤백한다")
    void rollsBackAfterFlushedSaveFailure(Change change) {
        assertThatThrownBy(() -> change(change, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Storage failed after flush");
        transaction()
                .executeWithoutResult(
                        status -> {
                            assertThat(
                                            entityManager
                                                    .createQuery(
                                                            "SELECT count(b) FROM BlackoutSchedule b WHERE b.roomId = :id",
                                                            Long.class)
                                                    .setParameter("id", roomId)
                                                    .getSingleResult())
                                    .isEqualTo(1L);
                            var stored = entityManager.find(BlackoutSchedule.class, id);
                            assertThat(stored.getReason()).isEqualTo("Maintenance");
                            assertThat(stored.getStartsAt()).isEqualTo(START);
                            assertThat(stored.getEndsAt()).isEqualTo(END);
                            assertThat(stored.getUpdatedAt()).isNull();
                            assertThat(stored.getDeletedAt()).isNull();
                        });
    }

    @Test
    @DisplayName("배타 락 조회는 트랜잭션 종료까지 다른 트랜잭션의 락 획득을 막는다")
    void exclusiveReadBlocksCompetingLockUntilTransactionEnds() {
        try (var executor = Executors.newSingleThreadExecutor()) {
            transaction()
                    .executeWithoutResult(
                            status -> {
                                adapter.findByIdWithExclusiveLock(id).orElseThrow();
                                var competing =
                                        executor.submit(
                                                () ->
                                                        transaction()
                                                                .executeWithoutResult(
                                                                        other -> {
                                                                            entityManager
                                                                                    .createNativeQuery(
                                                                                            "SET LOCAL lock_timeout = '300ms'")
                                                                                    .executeUpdate();
                                                                            adapter.findByIdWithExclusiveLock(
                                                                                            id)
                                                                                    .orElseThrow();
                                                                        }));
                                assertThatThrownBy(() -> competing.get(10, TimeUnit.SECONDS))
                                        .isInstanceOf(ExecutionException.class)
                                        .rootCause()
                                        .isInstanceOf(java.sql.SQLException.class)
                                        .extracting(
                                                error ->
                                                        ((java.sql.SQLException) error)
                                                                .getSQLState())
                                        .isEqualTo("55P03");
                            });
        }
        transaction()
                .executeWithoutResult(
                        status -> assertThat(adapter.findByIdWithExclusiveLock(id)).isPresent());
    }

    private void change(Change change, boolean fail) {
        var update = fail ? failingUpdateService : updateService;
        switch (change) {
            case CREATE ->
                    (fail ? failingCreateService : createService)
                            .create(
                                    CreateBlackoutScheduleCommand.of(
                                            roomId, scheduleId, range(), "Inspection"));
            case REASON ->
                    update.updateReason(UpdateBlackoutScheduleReasonCommand.of(id, "Inspection"));
            case PERIOD -> update.updateRange(UpdateBlackoutSchedulePeriodCommand.of(id, range()));
            case DELETE -> update.delete(DeleteBlackoutScheduleCommand.of(id));
        }
    }

    private InstantRange range() {
        return InstantRange.of(START.plusSeconds(60), END.plusSeconds(60));
    }

    private TransactionTemplate transaction() {
        var template = new TransactionTemplate(transactionManager);
        template.setTimeout(15);
        return template;
    }

    private enum Change {
        CREATE,
        REASON,
        PERIOD,
        DELETE
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        CreateBlackoutScheduleService createService(
                BlackoutScheduleJpaAdapter adapter,
                RoomJpaAdapter rooms,
                ScheduleJpaAdapter schedules) {
            return new CreateBlackoutScheduleService(
                    adapter, rooms, schedules, UUID::randomUUID, Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        CreateBlackoutScheduleService failingCreateService(
                BlackoutScheduleJpaAdapter adapter,
                RoomJpaAdapter rooms,
                ScheduleJpaAdapter schedules,
                EntityManager entityManager) {
            return new CreateBlackoutScheduleService(
                    failingSave(adapter, entityManager),
                    rooms,
                    schedules,
                    UUID::randomUUID,
                    Clock.fixed(NOW, ZoneOffset.UTC));
        }

        @Bean
        UpdateBlackoutScheduleService updateService(
                BlackoutScheduleJpaAdapter adapter,
                ScheduleJpaAdapter schedules,
                RoomJpaAdapter rooms) {
            return new UpdateBlackoutScheduleService(
                    adapter, adapter, schedules, adapter, Clock.fixed(NOW, ZoneOffset.UTC), rooms);
        }

        @Bean
        UpdateBlackoutScheduleService failingUpdateService(
                BlackoutScheduleJpaAdapter adapter,
                ScheduleJpaAdapter schedules,
                EntityManager entityManager,
                RoomJpaAdapter rooms) {
            return new UpdateBlackoutScheduleService(
                    adapter,
                    adapter,
                    schedules,
                    failingSave(adapter, entityManager),
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    rooms);
        }

        private BlackoutScheduleSavePort failingSave(
                BlackoutScheduleJpaAdapter adapter, EntityManager entityManager) {
            return blackout -> {
                adapter.save(blackout);
                entityManager.flush();
                throw new IllegalStateException("Storage failed after flush");
            };
        }
    }
}
