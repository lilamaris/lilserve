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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies blackout schema mappings, mutable attributes, and overlapping intervals against
 * PostgreSQL.
 */
@DisplayName("스케줄 일시 사용 불가 JPA 매핑")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class BlackoutScheduleJpaMappingTest {
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("차단 정보를 저장하고 수정한 뒤 조회하면 스키마의 모든 속성을 보존한다")
    void persistsAndUpdatesBlackout() {
        var createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var startsAt = Instant.parse("2026-02-02T01:00:00Z");
        var endsAt = startsAt.plusSeconds(3600);
        var blackout = persistBlackout(createdAt, startsAt, endsAt);
        var id = blackout.getId();
        var roomId = blackout.getRoomId();
        var scheduleId = blackout.getScheduleId();
        entityManager.clear();

        var stored = entityManager.find(BlackoutSchedule.class, id);
        assertThat(stored.getUpdatedAt()).isNull();
        assertThat(stored.getDeletedAt()).isNull();
        stored.updateReason("Inspection", createdAt.plusSeconds(1));
        stored.updatePeriod(startsAt.plusSeconds(1), endsAt, createdAt.plusSeconds(2));
        entityManager.flush();
        entityManager.clear();

        var found = entityManager.find(BlackoutSchedule.class, id);
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getRoomId()).isEqualTo(roomId);
        assertThat(found.getScheduleId()).isEqualTo(scheduleId);
        assertThat(found.getReason()).isEqualTo("Inspection");
        assertThat(found.getStartsAt()).isEqualTo(startsAt.plusSeconds(1));
        assertThat(found.getEndsAt()).isEqualTo(endsAt);
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isEqualTo(createdAt.plusSeconds(2));
    }

    @Test
    @DisplayName("삭제와 복원 시각을 데이터베이스에 저장하고 다시 조회한다")
    void persistsDeletionAndRestoration() {
        var createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var startsAt = Instant.parse("2026-02-02T01:00:00Z");
        var blackout = persistBlackout(createdAt, startsAt, startsAt.plusSeconds(3600));
        var id = blackout.getId();
        var deletedAt = createdAt.plusSeconds(1);
        blackout.delete(deletedAt);
        entityManager.flush();
        entityManager.clear();

        var deleted = entityManager.find(BlackoutSchedule.class, id);
        assertThat(deleted.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(deleted.getUpdatedAt()).isEqualTo(deletedAt);
        var restoredAt = deletedAt.plusSeconds(1);
        deleted.restore(restoredAt);
        entityManager.flush();
        entityManager.clear();

        var restored = entityManager.find(BlackoutSchedule.class, id);
        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.getUpdatedAt()).isEqualTo(restoredAt);
    }

    @Test
    @DisplayName("같은 방과 스케줄의 동일 구간에 서로 다른 사유의 차단을 저장할 수 있다")
    void allowsOverlappingBlackoutsForSameSchedule() {
        var createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var startsAt = Instant.parse("2026-02-02T01:00:00Z");
        var endsAt = startsAt.plusSeconds(3600);
        var first = persistBlackout(createdAt, startsAt, endsAt);
        var secondId = BlackoutScheduleId.of(UUID.randomUUID());
        entityManager.persistAndFlush(
                BlackoutSchedule.of(
                        secondId,
                        first.getRoomId(),
                        first.getScheduleId(),
                        "Inspection",
                        startsAt,
                        endsAt,
                        createdAt));
        entityManager.clear();

        var storedFirst = entityManager.find(BlackoutSchedule.class, first.getId());
        var storedSecond = entityManager.find(BlackoutSchedule.class, secondId);
        assertThat(storedFirst.getReason()).isEqualTo("Maintenance");
        assertThat(storedSecond.getReason()).isEqualTo("Inspection");
        assertThat(storedFirst.isDeleted()).isFalse();
        assertThat(storedSecond.isDeleted()).isFalse();
    }

    private BlackoutSchedule persistBlackout(Instant createdAt, Instant startsAt, Instant endsAt) {
        var roomId = RoomId.of(UUID.randomUUID());
        var scheduleId = ScheduleId.of(UUID.randomUUID());
        var id = BlackoutScheduleId.of(UUID.randomUUID());
        entityManager.persist(Room.of(roomId, "ROOM-01", "hall", "", createdAt));
        entityManager.persist(
                Schedule.of(
                        scheduleId,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        createdAt));
        entityManager.flush();
        return entityManager.persistAndFlush(
                BlackoutSchedule.of(
                        id, roomId, scheduleId, "Maintenance", startsAt, endsAt, createdAt));
    }
}
