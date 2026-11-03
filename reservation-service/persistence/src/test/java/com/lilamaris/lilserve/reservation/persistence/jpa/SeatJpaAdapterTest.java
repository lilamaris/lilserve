package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DisplayName("좌석 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(SeatJpaAdapter.class)
class SeatJpaAdapterTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-03T00:00:00Z");
    private static final Instant RESTORED_AT = Instant.parse("2026-01-04T00:00:00Z");

    /** The seat row carries a foreign key to room, so a room row is inserted before every seat. */
    private static final UUID ROOM_ID = UUID.randomUUID();

    @Autowired private SeatJpaAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("저장한 좌석을 ID로 찾는다")
    void findsSavedSeatById() {
        var seatId = persist(seat("SEAT-01"));

        var found = adapter.findById(seatId);

        assertThat(found).isPresent();
        var seat = found.orElseThrow();
        assertThat(seat.getId()).isEqualTo(seatId);
        assertThat(seat.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(seat.getCode()).isEqualTo("SEAT-01");
        assertThat(seat.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("삭제되지 않은 좌석은 삭제 제외 조회에 발견된다")
    void findsNotDeletedSeatById() {
        var seatId = persist(seat("SEAT-01"));

        assertThat(adapter.findNotDeletedById(seatId)).isPresent();
    }

    @Test
    @DisplayName("삭제된 좌석은 삭제 제외 조회에 발견되지 않는다")
    void skipsDeletedSeatInNotDeletedFind() {
        var seat = seat("SEAT-01");
        seat.delete(DELETED_AT);
        var seatId = persist(seat);

        assertThat(adapter.findNotDeletedById(seatId)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 좌석은 삭제 포함 조회에 발견된다")
    void findsDeletedSeatById() {
        var seat = seat("SEAT-01");
        seat.delete(DELETED_AT);
        var seatId = persist(seat);

        assertThat(adapter.findDeletedById(seatId)).map(Seat::getDeletedAt).contains(DELETED_AT);
    }

    @Test
    @DisplayName("삭제된 좌석의 행은 남으므로 전체 조회에 발견된다")
    void keepsDeletedSeatRowInFullFind() {
        var seat = seat("SEAT-01");
        seat.delete(DELETED_AT);
        var seatId = persist(seat);

        assertThat(adapter.findById(seatId)).map(Seat::isDeleted).contains(true);
    }

    @Test
    @DisplayName("삭제되지 않은 좌석은 삭제 조회에 발견되지 않는다")
    void skipsNotDeletedSeatInDeletedFind() {
        var seatId = persist(seat("SEAT-01"));

        assertThat(adapter.findDeletedById(seatId)).isEmpty();
    }

    @Test
    @DisplayName("저장한 좌석의 코드 변경이 영속화된다")
    void persistsCodeUpdateOnSave() {
        var seatId = persist(seat("SEAT-01"));
        var loaded = adapter.findById(seatId).orElseThrow();
        loaded.updateCode("SEAT-02", UPDATED_AT);

        adapter.save(loaded);
        flush();

        assertThat(adapter.findNotDeletedById(seatId)).map(Seat::getCode).contains("SEAT-02");
    }

    @Test
    @DisplayName("복원되면 삭제 시각이 지워지고 삭제 제외 조회에 발견된다")
    void clearsDeletedAtOnRestore() {
        var seat = seat("SEAT-01");
        seat.delete(DELETED_AT);
        var seatId = persist(seat);
        var deleted = adapter.findDeletedById(seatId).orElseThrow();
        deleted.restore(RESTORED_AT);

        adapter.save(deleted);
        flush();

        assertThat(adapter.findDeletedById(seatId)).isEmpty();
        assertThat(adapter.findNotDeletedById(seatId)).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 ID는 모든 조회에서 비어 있다")
    void returnsEmptyForUnknownId() {
        var missing = SeatId.of(UUID.randomUUID());

        assertThat(adapter.findById(missing)).isEmpty();
        assertThat(adapter.findNotDeletedById(missing)).isEmpty();
        assertThat(adapter.findDeletedById(missing)).isEmpty();
    }

    private Seat seat(String code) {
        return Seat.of(UUID.randomUUID(), ROOM_ID, code, CREATED_AT);
    }

    private SeatId persist(Seat seat) {
        seedRoom();
        adapter.save(seat);
        flush();
        return seat.getId();
    }

    private void seedRoom() {
        entityManager.persist(Room.of(ROOM_ID, "ROOM-01", "main hall", "capacity 100", CREATED_AT));
        flush();
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }
}
