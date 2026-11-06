package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Instant;
import java.time.LocalDate;
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

@DisplayName("예약 헤더 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(ReservationJpaAdapter.class)
class ReservationJpaAdapterTest {
    private static final Instant CREATED_AT = Instant.parse("2026-11-04T00:00:00Z");
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 11, 9);

    @Autowired private ReservationJpaAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("예약 헤더의 식별자와 룸과 좌석과 날짜와 시간대를 저장한다")
    void persistsReservationHeader() {
        var roomId = RoomId.of(UUID.randomUUID());
        var seatId = SeatId.of(UUID.randomUUID());
        entityManager.persist(Room.of(roomId, "ROOM-01", "hall", "test room", CREATED_AT));
        entityManager.persist(Seat.of(seatId, roomId, "SEAT-01", CREATED_AT));
        var reservation =
                Reservation.of(
                        ReservationId.of(UUID.randomUUID()),
                        roomId,
                        seatId,
                        SERVICE_DATE,
                        "Asia/Seoul",
                        CREATED_AT);

        adapter.save(reservation);
        entityManager.flush();
        entityManager.clear();

        var saved = entityManager.find(Reservation.class, reservation.getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(reservation.getId());
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getSeatId()).isEqualTo(seatId);
        assertThat(saved.getReservationDate()).isEqualTo(SERVICE_DATE);
        assertThat(saved.getTimeZone()).isEqualTo("Asia/Seoul");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getDeletedAt()).isNull();
    }
}
