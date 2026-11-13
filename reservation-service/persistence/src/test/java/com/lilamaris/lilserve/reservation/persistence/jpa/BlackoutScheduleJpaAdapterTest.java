package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies blackout persistence, scalar identifier projection, and protected reads against
 * PostgreSQL.
 */
@DisplayName("차단 스케줄 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(BlackoutScheduleJpaAdapter.class)
class BlackoutScheduleJpaAdapterTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = CREATED.plusSeconds(60);
    @Autowired private BlackoutScheduleJpaAdapter adapter;
    @Autowired private TestEntityManager entityManager;
    private BlackoutScheduleId id;
    private RoomId roomId;
    private ScheduleId scheduleId;

    @BeforeEach
    void setUp() {
        roomId = RoomId.of(UUID.randomUUID());
        scheduleId = ScheduleId.of(UUID.randomUUID());
        id = BlackoutScheduleId.of(UUID.randomUUID());
        entityManager.persist(Room.of(roomId, "ROOM-01", "hall", "", CREATED));
        entityManager.persist(
                Schedule.of(
                        scheduleId,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        CREATED));
        adapter.save(
                BlackoutSchedule.of(
                        id,
                        roomId,
                        scheduleId,
                        "Maintenance",
                        CREATED.plusSeconds(3600),
                        CREATED.plusSeconds(7200),
                        CREATED));
        flushAndClear();
    }

    @Test
    @DisplayName("저장한 차단의 모든 속성을 배타 락 조회로 읽는다")
    void readsSavedBlackout() {
        var found = adapter.findByIdWithExclusiveLock(id).orElseThrow();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getRoomId()).isEqualTo(roomId);
        assertThat(found.getScheduleId()).isEqualTo(scheduleId);
        assertThat(found.getReason()).isEqualTo("Maintenance");
        assertThat(found.getStartsAt()).isEqualTo(CREATED.plusSeconds(3600));
        assertThat(found.getEndsAt()).isEqualTo(CREATED.plusSeconds(7200));
        assertThat(found.getCreatedAt()).isEqualTo(CREATED);
        assertThat(found.getUpdatedAt()).isNull();
        assertThat(found.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("차단 식별자로 소속 스케줄 식별자를 조회한다")
    void findsParentScheduleIdentifier() {
        assertThat(adapter.findScheduleIdById(id)).contains(scheduleId);
    }

    @Test
    @DisplayName("없는 차단의 스케줄 식별자 조회는 빈 결과를 반환한다")
    void returnsEmptyIdentifierForMissingBlackout() {
        assertThat(adapter.findScheduleIdById(BlackoutScheduleId.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @DisplayName("없는 차단의 배타 락 조회는 빈 결과를 반환한다")
    void returnsEmptyLockForMissingBlackout() {
        assertThat(adapter.findByIdWithExclusiveLock(BlackoutScheduleId.of(UUID.randomUUID())))
                .isEmpty();
    }

    @Test
    @DisplayName("사유 변경을 저장하고 영속성 컨텍스트를 비워도 변경값을 보존한다")
    void persistsReasonChange() {
        var found = adapter.findByIdWithExclusiveLock(id).orElseThrow();
        entityManager.detach(found);
        found.updateReason("Inspection", NOW);
        adapter.save(found);
        flushAndClear();
        var stored = entityManager.find(BlackoutSchedule.class, id);
        assertThat(stored.getReason()).isEqualTo("Inspection");
        assertThat(stored.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("기간 변경을 저장하고 다시 읽는다")
    void persistsPeriodChange() {
        var found = adapter.findByIdWithExclusiveLock(id).orElseThrow();
        entityManager.detach(found);
        found.updatePeriod(NOW.plusSeconds(3600), NOW.plusSeconds(7200), NOW);
        adapter.save(found);
        flushAndClear();
        var stored = entityManager.find(BlackoutSchedule.class, id);
        assertThat(stored.getStartsAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(stored.getEndsAt()).isEqualTo(NOW.plusSeconds(7200));
        assertThat(stored.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("삭제된 차단도 락 조회와 부모 식별자 조회에 포함한다")
    void includesDeletedBlackoutInProtectedReads() {
        var found = adapter.findByIdWithExclusiveLock(id).orElseThrow();
        entityManager.detach(found);
        found.delete(NOW);
        adapter.save(found);
        flushAndClear();
        var stored = adapter.findByIdWithExclusiveLock(id).orElseThrow();
        assertThat(stored.getDeletedAt()).isEqualTo(NOW);
        assertThat(stored.getUpdatedAt()).isEqualTo(NOW);
        assertThat(adapter.findScheduleIdById(id)).contains(scheduleId);
    }

    @Test
    @DisplayName("부모 식별자 조회는 차단 엔티티를 캐싱하지 않아 이후 락 조회에서 변경값을 읽는다")
    void identifierProjectionDoesNotCacheBlackoutState() {
        assertThat(adapter.findScheduleIdById(id)).contains(scheduleId);
        entityManager
                .getEntityManager()
                .createNativeQuery(
                        "UPDATE blackout_schedule SET reason = 'Inspection' WHERE id = :id")
                .setParameter("id", id.getValue())
                .executeUpdate();
        assertThat(adapter.findByIdWithExclusiveLock(id))
                .map(BlackoutSchedule::getReason)
                .contains("Inspection");
    }

    @Test
    @DisplayName("같은 슬롯에서 기간이 겹치는 활성 차단을 모두 반환한다")
    void returnsAllOverlappingRoomBlackouts() {
        var first = persistBlackout(roomId, scheduleId, CREATED, NOW.plusSeconds(60));
        var second = persistBlackout(roomId, scheduleId, CREATED, NOW.plusSeconds(120));
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW))
                .extracting(BlackoutSchedule::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    @DisplayName("차단 시작 시각은 방의 활성 차단 조회에 포함한다")
    void includesBlackoutAtStart() {
        var blackout = persistBlackout(roomId, scheduleId, NOW, NOW.plusSeconds(60));
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW))
                .extracting(BlackoutSchedule::getId)
                .containsExactly(blackout.getId());
    }

    @Test
    @DisplayName("차단 종료 시각은 방의 활성 차단 조회에서 제외한다")
    void excludesBlackoutAtEnd() {
        persistBlackout(roomId, scheduleId, CREATED, NOW);
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW)).isEmpty();
    }

    @Test
    @DisplayName("종료된 차단은 방의 활성 차단 조회에서 제외한다")
    void excludesExpiredRoomBlackout() {
        persistBlackout(roomId, scheduleId, CREATED, NOW.minusSeconds(1));
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW)).isEmpty();
    }

    @Test
    @DisplayName("시작 전 차단은 방의 활성 차단 조회에서 제외한다")
    void excludesFutureRoomBlackout() {
        // The fixture blackout starts one hour after creation.
        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 차단은 유효 기간 안이어도 방의 활성 차단 조회에서 제외한다")
    void excludesDeletedRoomBlackout() {
        var blackout = persistBlackout(roomId, scheduleId, CREATED, NOW.plusSeconds(60));
        blackout.delete(NOW);
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW)).isEmpty();
    }

    @Test
    @DisplayName("다른 방의 활성 차단은 조회에 포함하지 않는다")
    void excludesOtherRoomBlackout() {
        var otherRoom = RoomId.of(UUID.randomUUID());
        var otherSchedule = ScheduleId.of(UUID.randomUUID());
        entityManager.persist(Room.of(otherRoom, "ROOM-02", "hall", "", CREATED));
        entityManager.persist(
                Schedule.of(
                        otherSchedule,
                        otherRoom,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        CREATED));
        persistBlackout(otherRoom, otherSchedule, CREATED, NOW.plusSeconds(60));
        flushAndClear();

        assertThat(adapter.findNotDeletedByRoomIdAt(roomId, NOW)).isEmpty();
    }

    @Test
    @DisplayName("점검 식별자로 등록된 방을 엔티티 로딩 없이 조회한다")
    void findsOwningRoomIdentifier() {
        assertThat(adapter.findRoomIdById(id)).contains(roomId);
    }

    @Test
    @DisplayName("없는 점검의 방 식별자 조회는 빈 결과를 반환한다")
    void returnsEmptyRoomForMissingBlackout() {
        assertThat(adapter.findRoomIdById(BlackoutScheduleId.of(UUID.randomUUID()))).isEmpty();
    }

    private BlackoutSchedule persistBlackout(
            RoomId owner, ScheduleId slot, Instant start, Instant end) {
        var blackout =
                BlackoutSchedule.of(
                        BlackoutScheduleId.of(UUID.randomUUID()),
                        owner,
                        slot,
                        "Maintenance",
                        start,
                        end,
                        CREATED);
        entityManager.persist(blackout);
        return blackout;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
