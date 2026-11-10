package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("룸 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(RoomJpaAdapter.class)
class RoomJpaAdapterTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-03T00:00:00Z");
    private static final Instant RESTORED_AT = Instant.parse("2026-01-04T00:00:00Z");

    @Autowired private RoomJpaAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("저장한 룸을 ID로 찾는다")
    void findsSavedRoomById() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));

        var found = adapter.findById(roomId);

        assertThat(found).isPresent();
        var room = found.orElseThrow();
        assertThat(room.getId()).isEqualTo(roomId);
        assertThat(room.getCode()).isEqualTo("ROOM-01");
        assertThat(room.getName()).isEqualTo("main hall");
        assertThat(room.getDescription()).isEqualTo("capacity 100");
        assertThat(room.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 룸은 삭제 제외 조회에 발견된다")
    void findsNotDeletedRoomById() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));

        assertThat(adapter.findNotDeletedById(roomId)).isPresent();
    }

    @Test
    @DisplayName("삭제된 룸은 삭제 제외 조회에 발견되지 않는다")
    void skipsDeletedRoomInNotDeletedFind() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);

        assertThat(adapter.findNotDeletedById(roomId)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 룸은 삭제 포함 조회에 발견된다")
    void findsDeletedRoomById() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);

        assertThat(adapter.findDeletedById(roomId)).map(Room::getDeletedAt).contains(DELETED_AT);
    }

    @Test
    @DisplayName("삭제된 룸의 행은 남으므로 전체 조회에 발견된다")
    void keepsDeletedRoomRowInFullFind() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);

        assertThat(adapter.findById(roomId)).map(Room::isDeleted).contains(true);
    }

    @Test
    @DisplayName("삭제되지 않은 룸은 삭제 조회에 발견되지 않는다")
    void skipsNotDeletedRoomInDeletedFind() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));

        assertThat(adapter.findDeletedById(roomId)).isEmpty();
    }

    @Test
    @DisplayName("저장한 룸의 이름 변경이 영속화된다")
    void persistsNameUpdateOnSave() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));
        var loaded = adapter.findById(roomId).orElseThrow();
        loaded.updateName("renamed hall", UPDATED_AT);

        adapter.save(loaded);
        flush();

        assertThat(adapter.findNotDeletedById(roomId)).map(Room::getName).contains("renamed hall");
    }

    @Test
    @DisplayName("복원되면 삭제 시각이 지워지고 삭제 제외 조회에 발견된다")
    void clearsDeletedAtOnRestore() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);
        var deleted = adapter.findDeletedById(roomId).orElseThrow();
        deleted.restore(RESTORED_AT);

        adapter.save(deleted);
        flush();

        assertThat(adapter.findDeletedById(roomId)).isEmpty();
        assertThat(adapter.findNotDeletedById(roomId)).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 ID는 모든 조회에서 비어 있다")
    void returnsEmptyForUnknownId() {
        var missing = RoomId.of(UUID.randomUUID());

        assertThat(adapter.findById(missing)).isEmpty();
        assertThat(adapter.findNotDeletedById(missing)).isEmpty();
        assertThat(adapter.findDeletedById(missing)).isEmpty();
    }

    @Test
    @DisplayName("존재하는 방은 공유 락과 함께 반환된다")
    void returnsRoomWithSharedLock() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));

        assertThat(adapter.findByIdWithSharedLock(roomId)).map(Room::getId).contains(roomId);
    }

    @Test
    @DisplayName("삭제된 방도 공유 락 대상이므로 반환된다")
    void returnsDeletedRoomWithSharedLock() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);

        assertThat(adapter.findByIdWithSharedLock(roomId)).map(Room::isDeleted).contains(true);
    }

    @Test
    @DisplayName("존재하지 않는 방의 공유 락 획득은 빈 결과로 알린다")
    void reportsEmptySharedLockForUnknownId() {
        var missing = RoomId.of(UUID.randomUUID());

        assertThat(adapter.findByIdWithSharedLock(missing)).isEmpty();
    }

    @Test
    @DisplayName("존재하는 방은 배타 락과 함께 반환된다")
    void returnsRoomWithExclusiveLock() {
        var roomId = persist(room("ROOM-01", "main hall", "capacity 100"));

        assertThat(adapter.findByIdWithExclusiveLock(roomId)).map(Room::getId).contains(roomId);
    }

    @Test
    @DisplayName("삭제된 방도 배타 락 대상이므로 반환된다")
    void returnsDeletedRoomWithExclusiveLock() {
        var room = room("ROOM-01", "main hall", "capacity 100");
        room.delete(DELETED_AT);
        var roomId = persist(room);

        assertThat(adapter.findByIdWithExclusiveLock(roomId)).map(Room::isDeleted).contains(true);
    }

    @Test
    @DisplayName("존재하지 않는 방의 배타 락 획득은 빈 결과로 알린다")
    void reportsEmptyExclusiveLockForUnknownId() {
        var missing = RoomId.of(UUID.randomUUID());

        assertThat(adapter.findByIdWithExclusiveLock(missing)).isEmpty();
    }

    private Room room(String code, String name, String description) {
        return Room.of(RoomId.of(UUID.randomUUID()), code, name, description, CREATED_AT);
    }

    private RoomId persist(Room room) {
        adapter.save(room);
        flush();
        return room.getId();
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }
}
