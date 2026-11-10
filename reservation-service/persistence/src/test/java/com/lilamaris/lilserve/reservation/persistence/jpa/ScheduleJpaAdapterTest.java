package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ScheduleRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("스케줄 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(ScheduleJpaAdapter.class)
class ScheduleJpaAdapterTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant RETIRED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired private ScheduleJpaAdapter adapter;
    @Autowired private ScheduleRepository repository;
    @Autowired private TestEntityManager entityManager;
    private final RoomId roomId = RoomId.of(UUID.randomUUID());

    @BeforeEach
    void seedRoom() {
        entityManager.persist(Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
        flushAndClear();
    }

    @Test
    @DisplayName("저장한 활성 스케줄을 식별자로 조회하면 시간 정의와 생성 시각을 보존한다")
    void savesAndFindsActiveSchedule() {
        var schedule = schedule(10, 12);

        adapter.save(schedule);
        flushAndClear();

        var found = adapter.findNotDeletedById(schedule.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(schedule.getId());
        assertThat(found.getRoomId()).isEqualTo(roomId);
        assertThat(found.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(found.getStartAt()).isEqualTo(LocalTime.of(10, 0));
        assertThat(found.getEndAt()).isEqualTo(LocalTime.of(12, 0));
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("여러 인접 스케줄을 일괄 저장하면 각각 다시 조회할 수 있다")
    void savesAllSchedules() {
        var morning = schedule(10, 12);
        var afternoon = schedule(12, 14);

        adapter.saveAll(List.of(morning, afternoon));
        flushAndClear();

        assertThat(adapter.findNotDeletedById(morning.getId()))
                .map(Schedule::getStartAt)
                .contains(LocalTime.of(10, 0));
        assertThat(adapter.findNotDeletedById(afternoon.getId()))
                .map(Schedule::getStartAt)
                .contains(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("이미 폐기된 스케줄은 활성 조회에서 제외한다")
    void excludesRetiredSchedule() {
        var schedule = schedule(10, 12);
        schedule.delete(RETIRED_AT);
        entityManager.persistAndFlush(schedule);
        entityManager.clear();

        assertThat(adapter.findNotDeletedById(schedule.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 스케줄은 빈 결과를 반환한다")
    void returnsEmptyForUnknownId() {
        assertThat(adapter.findNotDeletedById(ScheduleId.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    @DisplayName("폐기는 행과 원본 시간 정의를 보존하고 활성 조회에서 제외한다")
    void retiresScheduleWithoutChangingItsDefinition() {
        var id = persist(schedule(10, 12));

        adapter.retire(id, RETIRED_AT);
        flushAndClear();

        assertThat(adapter.findNotDeletedById(id)).isEmpty();
        var retired = entityManager.find(Schedule.class, id);
        assertThat(retired).isNotNull();
        assertThat(retired.getDeletedAt()).isEqualTo(RETIRED_AT);
        assertThat(retired.getRoomId()).isEqualTo(roomId);
        assertThat(retired.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(retired.getStartAt()).isEqualTo(LocalTime.of(10, 0));
        assertThat(retired.getEndAt()).isEqualTo(LocalTime.of(12, 0));
        assertThat(retired.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("폐기를 반복해도 최초 폐기 시각을 덮어쓰지 않는다")
    void preservesFirstRetirementTime() {
        var id = persist(schedule(10, 12));
        adapter.retire(id, RETIRED_AT);
        flushAndClear();

        adapter.retire(id, RETIRED_AT.plusSeconds(60));
        flushAndClear();

        assertThat(entityManager.find(Schedule.class, id).getDeletedAt()).isEqualTo(RETIRED_AT);
    }

    @Test
    @DisplayName("선택한 스케줄을 폐기해도 다른 스케줄은 활성 상태를 유지한다")
    void retiresOnlySelectedSchedule() {
        var selected = persist(schedule(10, 12));
        var other = persist(schedule(14, 16));

        adapter.retire(selected, RETIRED_AT);
        flushAndClear();

        assertThat(adapter.findNotDeletedById(selected)).isEmpty();
        assertThat(adapter.findNotDeletedById(other))
                .hasValueSatisfying(schedule -> assertThat(schedule.getDeletedAt()).isNull());
    }

    @Test
    @DisplayName("없는 스케줄을 폐기해도 기존 활성 스케줄은 변경되지 않는다")
    void ignoresRetirementOfUnknownSchedule() {
        var existing = persist(schedule(10, 12));

        adapter.retire(ScheduleId.of(UUID.randomUUID()), RETIRED_AT);
        flushAndClear();

        assertThat(adapter.findNotDeletedById(existing)).isPresent();
    }

    @Test
    @DisplayName("생성 시각보다 이른 폐기는 데이터베이스 제약으로 거부한다")
    void rejectsRetirementBeforeCreation() {
        var id = persist(schedule(10, 12));

        assertThatThrownBy(() -> adapter.retire(id, CREATED_AT.minusSeconds(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("chk_schedule_deleted_at");
    }

    @Test
    @DisplayName("활성 스케줄과 겹치는 시간은 데이터베이스 제약으로 저장을 거부한다")
    void rejectsOverlappingActiveSchedule() {
        persist(schedule(10, 12));

        assertThatThrownBy(
                        () -> {
                            adapter.save(schedule(11, 13));
                            entityManager.flush();
                        })
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ex_schedule_no_overlap");
    }

    @Test
    @DisplayName("원본 폐기 후에는 원본과 겹치는 대체 스케줄을 저장할 수 있다")
    void savesReplacementOverlappingRetiredSchedule() {
        var original = persist(schedule(10, 12));
        var replacement = schedule(11, 13);

        adapter.retire(original, RETIRED_AT);
        adapter.save(replacement);
        flushAndClear();

        assertThat(adapter.findNotDeletedById(original)).isEmpty();
        assertThat(adapter.findNotDeletedById(replacement.getId())).isPresent();
    }

    @Test
    @DisplayName("요청한 슬롯 전체가 공유 락과 함께 반환된다")
    void returnsLockedScheduleIdentifiers() {
        var morning = persist(schedule(10, 12));
        var afternoon = persist(schedule(14, 16));

        var locked = adapter.findAllByIdsWithSharedLock(Set.of(afternoon, morning)).orElseThrow();

        assertThat(locked).containsExactlyInAnyOrder(morning, afternoon);
    }

    @Test
    @DisplayName("삭제된 슬롯도 공유 락 대상이므로 반환된다")
    void returnsRetiredScheduleWithSharedLock() {
        var retired = persist(schedule(10, 12));
        adapter.retire(retired, RETIRED_AT);
        flushAndClear();

        var locked = adapter.findAllByIdsWithSharedLock(Set.of(retired)).orElseThrow();

        assertThat(locked).containsExactly(retired);
    }

    @Test
    @DisplayName("요청한 스케줄 중 하나라도 없으면 공유 락 획득을 빈 결과로 알린다")
    void reportsEmptySharedLocksWhenAnyRequestedScheduleIsMissing() {
        var existing = persist(schedule(10, 12));
        var missing = ScheduleId.of(UUID.randomUUID());

        assertThat(adapter.findAllByIdsWithSharedLock(Set.of(existing, missing))).isEmpty();
    }

    @Test
    @DisplayName("스케줄 공유 락 조회는 삽입 순서와 관계없이 식별자 오름차순으로 처리한다")
    void loadsSharedLocksInIdentifierOrder() {
        var lower = ScheduleId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var higher = ScheduleId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        persist(
                Schedule.of(
                        higher,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        CREATED_AT));
        persist(
                Schedule.of(
                        lower,
                        roomId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        CREATED_AT));
        // A sequential scan exposes insertion order unless the locking query explicitly sorts.
        entityManager
                .getEntityManager()
                .createNativeQuery("SET LOCAL enable_indexscan = off")
                .executeUpdate();
        entityManager
                .getEntityManager()
                .createNativeQuery("SET LOCAL enable_bitmapscan = off")
                .executeUpdate();

        var locked = repository.findByIdsWithSharedLock(List.of(higher, lower));

        assertThat(locked).extracting(Schedule::getId).containsExactly(lower, higher);
    }

    @Test
    @DisplayName("존재하는 스케줄은 배타 락과 함께 반환된다")
    void returnsScheduleWithExclusiveLock() {
        var scheduleId = persist(schedule(10, 12));

        assertThat(adapter.findByIdWithExclusiveLock(scheduleId))
                .map(Schedule::getId)
                .contains(scheduleId);
    }

    @Test
    @DisplayName("폐기된 스케줄도 배타 락 대상이므로 반환된다")
    void returnsRetiredScheduleWithExclusiveLock() {
        var scheduleId = persist(schedule(10, 12));
        adapter.retire(scheduleId, RETIRED_AT);
        flushAndClear();

        assertThat(adapter.findByIdWithExclusiveLock(scheduleId))
                .map(Schedule::getDeletedAt)
                .contains(RETIRED_AT);
    }

    @Test
    @DisplayName("존재하지 않는 스케줄의 배타 락 획득은 빈 결과로 알린다")
    void reportsEmptyExclusiveLockForUnknownId() {
        assertThat(adapter.findByIdWithExclusiveLock(ScheduleId.of(UUID.randomUUID()))).isEmpty();
    }

    private Schedule schedule(int start, int end) {
        return Schedule.of(
                ScheduleId.of(UUID.randomUUID()),
                roomId,
                DayOfWeek.MONDAY,
                LocalTime.of(start, 0),
                LocalTime.of(end, 0),
                CREATED_AT);
    }

    private ScheduleId persist(Schedule schedule) {
        entityManager.persistAndFlush(schedule);
        entityManager.clear();
        return schedule.getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
