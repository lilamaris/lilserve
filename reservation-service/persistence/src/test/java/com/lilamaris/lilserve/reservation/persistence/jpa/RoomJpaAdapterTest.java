package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource("roomFilters")
    @DisplayName("방 이름과 코드 부분 일치 필터는 대소문자 구분 없이 결합되고 삭제된 방은 제외한다")
    void filtersActiveRooms(String name, String code, List<String> expected) {
        seedListRooms();

        var page = adapter.findNotDeletedByFilters(name, code, OffsetPageRequest.of(0, 10));

        assertThat(page.items()).extracting(Room::getCode).containsExactlyElementsOf(expected);
        assertThat(page.totalElements()).isEqualTo(expected.size());
    }

    private static Stream<Arguments> roomFilters() {
        return Stream.of(
                Arguments.of(null, null, List.of("A-01", "A-02", "B-01")),
                Arguments.of("mAIN", null, List.of("A-01", "B-01")),
                Arguments.of(null, "a-", List.of("A-01", "A-02")),
                Arguments.of("main", "a-", List.of("A-01")),
                Arguments.of("quiet", "B-", List.of()),
                Arguments.of("missing", null, List.of()));
    }

    @ParameterizedTest
    @MethodSource("roomPages")
    @DisplayName("방 목록은 코드순으로 페이지를 나누고 범위 밖에서도 전체 건수를 보존한다")
    void paginatesActiveRooms(int number, List<String> expected) {
        seedListRooms();

        var page = adapter.findNotDeletedByFilters(null, null, OffsetPageRequest.of(number, 2));

        assertThat(page.items()).extracting(Room::getCode).containsExactlyElementsOf(expected);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.page()).isEqualTo(number);
        assertThat(page.size()).isEqualTo(2);
    }

    private static Stream<Arguments> roomPages() {
        return Stream.of(
                Arguments.of(0, List.of("A-01", "A-02")),
                Arguments.of(1, List.of("B-01")),
                Arguments.of(2, List.of()));
    }

    @Test
    @DisplayName("검색어의 퍼센트와 밑줄은 와일드카드가 아닌 실제 문자로 처리한다")
    void treatsFilterWildcardsLiterally() {
        persist(room("A%_01", "Hall%_", ""));
        persist(room("AB01", "HallAB", ""));

        var page = adapter.findNotDeletedByFilters("%_", "%_", OffsetPageRequest.of(0, 10));

        assertThat(page.items()).extracting(Room::getCode).containsExactly("A%_01");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("필터가 적용된 페이지의 전체 건수는 일치하는 방만 센다")
    void countsOnlyFilteredRoomsAcrossPages() {
        seedListRooms();

        var page = adapter.findNotDeletedByFilters("main", null, OffsetPageRequest.of(0, 1));

        assertThat(page.items()).extracting(Room::getCode).containsExactly("A-01");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    private void seedListRooms() {
        persist(room("B-01", "Main annex", ""));
        persist(room("A-02", "Quiet room", ""));
        persist(room("A-01", "Main hall", ""));
        var deleted = room("D-01", "Main hall", "");
        deleted.delete(DELETED_AT);
        persist(deleted);
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
