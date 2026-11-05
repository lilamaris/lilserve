package com.lilamaris.lilserve.reservation.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilserve.reservation.application.port.out.ReserveScheduleStatus;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.ReservedSchedule;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;

@DisplayName("예약 슬롯 JPA 어댑터")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PersistenceJpaTestConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({ReservedScheduleJpaAdapter.class, ReservationJpaAdapter.class})
class ReservedScheduleJpaAdapterTest {
    private static final Instant SLOT_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant RESERVED_AT = Instant.parse("2026-11-04T00:00:00Z");
    private static final Instant RETIRED_AT = Instant.parse("2026-11-05T00:00:00Z");
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 11, 9);
    private static final String TIME_ZONE = "Asia/Seoul";

    @Autowired private ReservedScheduleJpaAdapter adapter;
    @Autowired private ReservationJpaAdapter reservationAdapter;
    @Autowired private TestEntityManager entityManager;

    private Room room;
    private Seat seat;

    @BeforeEach
    void persistsRoomAndSeat() {
        room = persistRoom();
        seat = persistSeat();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    @DisplayName("예약 조회는 첫 점유 시작 직전까지만 허용하고 시작 시각부터 거부한다")
    void findsReservationOnlyBeforeEarliestAllocationStarts(long secondsFromStart) {
        var morning = persistSchedule(10, 12);
        var afternoon = persistSchedule(14, 16);
        var reservation = saveReservation();
        adapter.reserve(reservation.getId(), Set.of(morning.getId(), afternoon.getId()));
        var now = Instant.parse("2026-11-09T01:00:00Z").plusSeconds(secondsFromStart);
        entityManager.clear();

        var found = reservationAdapter.findNotStartedById(reservation.getId(), now);

        if (secondsFromStart < 0) {
            assertThat(found).isPresent();
            assertThat(found.orElseThrow().getId()).isEqualTo(reservation.getId());
        } else {
            assertThat(found).isEmpty();
        }
    }

    @Test
    @DisplayName("다른 예약의 과거 점유가 있어도 대상 예약의 미래 점유만으로 조회한다")
    void findsFutureReservationIndependentlyOfOtherReservations() {
        var schedule = persistSchedule(10, 12);
        var earlier = saveReservation();
        adapter.reserve(earlier.getId(), Set.of(schedule.getId()));
        var future = saveReservation(seat, SERVICE_DATE.plusWeeks(1));
        adapter.reserve(future.getId(), Set.of(schedule.getId()));
        entityManager.clear();

        var found =
                reservationAdapter.findNotStartedById(
                        future.getId(), Instant.parse("2026-11-16T00:00:00Z"));

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(future.getId());
    }

    @Test
    @DisplayName("존재하지 않는 예약은 조회하지 않는다")
    void doesNotFindMissingReservation() {
        assertThat(
                        reservationAdapter.findNotStartedById(
                                ReservationId.of(UUID.randomUUID()), RESERVED_AT))
                .isEmpty();
    }

    @Test
    @DisplayName("활성 점유가 없는 예약 헤더는 조회하지 않는다")
    void doesNotFindReservationWithoutAllocations() {
        var reservation = saveReservation();
        entityManager.flush();
        entityManager.clear();

        assertThat(reservationAdapter.findNotStartedById(reservation.getId(), RETIRED_AT))
                .isEmpty();
    }

    @Test
    @DisplayName("미래 점유가 있어도 취소된 예약 헤더는 조회하지 않는다")
    void doesNotFindCancelledHeaderWithFutureAllocations() {
        var schedule = persistSchedule(10, 12);
        var reservation = saveReservation();
        adapter.reserve(reservation.getId(), Set.of(schedule.getId()));
        reservation.delete(RETIRED_AT);
        reservationAdapter.save(reservation);
        entityManager.flush();
        entityManager.clear();

        assertThat(reservationAdapter.findNotStartedById(reservation.getId(), RETIRED_AT))
                .isEmpty();
    }

    @Test
    @DisplayName("모든 점유가 해제된 예약은 조회하지 않는다")
    void doesNotFindReservationWithOnlyReleasedAllocations() {
        var schedule = persistSchedule(10, 12);
        var reservation = saveReservation();
        adapter.reserve(reservation.getId(), Set.of(schedule.getId()));
        adapter.release(reservation.getId(), RETIRED_AT);
        entityManager.clear();

        assertThat(reservationAdapter.findNotStartedById(reservation.getId(), RETIRED_AT))
                .isEmpty();
    }

    @Test
    @DisplayName("전체 점유 해제는 대상 예약만 취소하고 다른 예약의 점유는 유지한다")
    void releasesAllAllocationsWithoutChangingOtherReservations() {
        var morning = persistSchedule(10, 12);
        var afternoon = persistSchedule(14, 16);
        var reservation = saveReservation();
        adapter.reserve(reservation.getId(), Set.of(morning.getId(), afternoon.getId()));
        var otherReservation = saveReservation(persistSeat(), SERVICE_DATE);
        adapter.reserve(otherReservation.getId(), Set.of(morning.getId()));

        adapter.release(reservation.getId(), RETIRED_AT);
        entityManager.clear();

        var released = allocations(reservation.getId());
        assertThat(released).hasSize(2);
        assertThat(released)
                .allSatisfy(
                        allocation -> assertThat(allocation.getDeletedAt()).isEqualTo(RETIRED_AT));
        assertThat(allocations(otherReservation.getId()))
                .singleElement()
                .satisfies(allocation -> assertThat(allocation.getDeletedAt()).isNull());
    }

    @Test
    @DisplayName("점유 해제를 재호출해도 최초 취소 시각을 유지한다")
    void preservesOriginalCancellationTimeWhenReleasedAgain() {
        var schedule = persistSchedule(10, 12);
        var reservation = saveReservation();
        adapter.reserve(reservation.getId(), Set.of(schedule.getId()));
        adapter.release(reservation.getId(), RETIRED_AT);

        adapter.release(reservation.getId(), RETIRED_AT.plusSeconds(60));
        entityManager.clear();

        assertThat(allocations(reservation.getId()))
                .singleElement()
                .satisfies(
                        allocation -> assertThat(allocation.getDeletedAt()).isEqualTo(RETIRED_AT));
    }

    @Test
    @DisplayName("요청한 모든 슬롯을 예약하면 전체 성공을 반환하고 시간과 예약 범위를 복사한다")
    void reservesAllRequestedSchedulesWithSnapshots() {
        var morning = persistSchedule(10, 12);
        var afternoon = persistSchedule(14, 16);
        var reservation = saveReservation();

        var status =
                adapter.reserve(reservation.getId(), Set.of(morning.getId(), afternoon.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        var saved = allocations(reservation.getId());
        assertThat(saved)
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(morning.getId(), afternoon.getId());
        assertThat(saved)
                .extracting(ReservedSchedule::getId)
                .doesNotHaveDuplicates()
                .doesNotContainNull();
        assertThat(saved)
                .allSatisfy(
                        allocation -> {
                            assertThat(allocation.getReservationId())
                                    .isEqualTo(reservation.getId());
                            assertThat(allocation.getRoomId()).isEqualTo(room.getId());
                            assertThat(allocation.getSeatId()).isEqualTo(seat.getId());
                            assertThat(allocation.getReservationDate()).isEqualTo(SERVICE_DATE);
                            assertThat(allocation.getTimeZone()).isEqualTo(TIME_ZONE);
                            assertThat(allocation.getCreatedAt()).isEqualTo(RESERVED_AT);
                            assertThat(allocation.getDeletedAt()).isNull();
                            assertThat(allocation.getId().getValue().version()).isEqualTo(7);
                            assertThat(allocation.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                        });
        assertThat(saved.getFirst().getStartAt()).isEqualTo(morning.getStartAt());
        assertThat(saved.getFirst().getEndAt()).isEqualTo(morning.getEndAt());
        assertThat(saved.getFirst().getStartsAt()).isEqualTo(Instant.parse("2026-11-09T01:00:00Z"));
        assertThat(saved.getFirst().getEndsAt()).isEqualTo(Instant.parse("2026-11-09T03:00:00Z"));
        assertThat(saved.getLast().getStartAt()).isEqualTo(afternoon.getStartAt());
        assertThat(saved.getLast().getEndAt()).isEqualTo(afternoon.getEndAt());
        assertThat(saved.getLast().getStartsAt()).isEqualTo(Instant.parse("2026-11-09T05:00:00Z"));
        assertThat(saved.getLast().getEndsAt()).isEqualTo(Instant.parse("2026-11-09T07:00:00Z"));
    }

    @Test
    @DisplayName("헤더 저장을 명시적으로 플러시하지 않아도 슬롯 점유를 저장한다")
    void reservesUsingPendingReservationHeader() {
        var schedule = persistSchedule(10, 12);
        entityManager.flush();
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(entityManager.find(Reservation.class, reservation.getId())).isNotNull();
        assertThat(allocations(reservation.getId())).hasSize(1);
    }

    @Test
    @DisplayName("요청하지 않은 슬롯은 예약하지 않는다")
    void reservesOnlySelectedSchedules() {
        var selected = persistSchedule(10, 12);
        persistSchedule(14, 16);
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(selected.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(selected.getId());
    }

    @ParameterizedTest
    @EnumSource(UnavailableSchedule.class)
    @DisplayName("요청 중 일부 슬롯만 예약할 수 있으면 실패를 반환한다")
    void returnsFailedWhenOnlySomeSchedulesAreAvailable(UnavailableSchedule reason) {
        var available = persistSchedule(10, 12);
        var unavailable = unavailableSchedule(reason);
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(available.getId(), unavailable));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(available.getId());
    }

    @ParameterizedTest
    @EnumSource(UnavailableSchedule.class)
    @DisplayName("제약 충돌 없이 예약 가능한 슬롯이 없어도 실패를 반환한다")
    void returnsFailedWhenNoSchedulesAreAvailable(UnavailableSchedule reason) {
        var unavailable = unavailableSchedule(reason);
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(unavailable));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 좌석과 날짜에 이미 예약된 슬롯은 실패를 반환한다")
    void returnsFailedForAlreadyReservedSchedule() {
        var schedule = persistSchedule(10, 12);
        persistAllocation(saveReservation(), schedule);
        var attempted = saveReservation();

        var status = adapter.reserve(attempted.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(attempted.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 헤더로 같은 슬롯을 다시 요청해도 기존 점유를 변경하거나 중복 생성하지 않는다")
    void rejectsRepeatedReservationWithoutChangingAllocation() {
        var schedule = persistSchedule(10, 12);
        var reservation = saveReservation();
        var original = persistAllocation(reservation, schedule);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getId)
                .containsExactly(original.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Asia/Seoul", "UTC"})
    @DisplayName("점유의 확정 시각은 예약 헤더에 저장된 시간대를 기준으로 계산한다")
    void derivesOccupiedInstantsFromHeaderTimezone(String timezone) {
        var schedule = persistSchedule(10, 12);
        var reservation =
                Reservation.of(
                        UUID.randomUUID(),
                        room.getId().getValue(),
                        seat.getId().getValue(),
                        SERVICE_DATE,
                        timezone,
                        RESERVED_AT);
        reservationAdapter.save(reservation);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        var zone = ZoneId.of(timezone);
        assertThat(allocations(reservation.getId()))
                .singleElement()
                .satisfies(
                        allocation -> {
                            assertThat(allocation.getTimeZone()).isEqualTo(timezone);
                            assertThat(allocation.getStartsAt())
                                    .isEqualTo(SERVICE_DATE.atTime(10, 0).atZone(zone).toInstant());
                            assertThat(allocation.getEndsAt())
                                    .isEqualTo(SERVICE_DATE.atTime(12, 0).atZone(zone).toInstant());
                        });
    }

    @Test
    @DisplayName("요청 중 하나라도 기존 점유와 충돌하면 전체 실패를 반환한다")
    void returnsFailedWhenAnyRequestedScheduleConflicts() {
        var occupied = persistSchedule(10, 12);
        var available = persistSchedule(14, 16);
        var existing = saveReservation();
        var allocation = persistAllocation(existing, occupied);
        var attempted = saveReservation();

        var status =
                adapter.reserve(attempted.getId(), Set.of(available.getId(), occupied.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(attempted.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(available.getId());
        assertThat(allocations(existing.getId()))
                .extracting(ReservedSchedule::getId)
                .containsExactly(allocation.getId());
    }

    @Test
    @DisplayName("슬롯 버전이 달라도 같은 좌석의 이용 시간이 겹치면 실패를 반환한다")
    void returnsFailedForOverlapWithRetiredScheduleAllocation() {
        var original = persistSchedule(10, 12);
        persistAllocation(saveReservation(), original);
        original.delete(RETIRED_AT);
        entityManager.flush();
        var replacement = persistSchedule(11, 13);
        var available = persistSchedule(14, 16);
        var attempted = saveReservation();

        var status =
                adapter.reserve(attempted.getId(), Set.of(available.getId(), replacement.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(attempted.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(available.getId());
    }

    @Test
    @DisplayName("헤더 플러시 중 좌석과 룸의 외래 키 위반이 발생하면 예외를 전파한다")
    void propagatesReservationSeatRoomConstraintViolation() {
        var otherRoom = persistRoom();
        var schedule = persistSchedule(otherRoom.getId(), DayOfWeek.MONDAY, 10, 12);
        entityManager.flush();
        var invalid = reservation(otherRoom.getId(), seat.getId(), SERVICE_DATE);
        reservationAdapter.save(invalid);

        assertThatThrownBy(() -> adapter.reserve(invalid.getId(), Set.of(schedule.getId())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("fk_reservation_seat_room");
    }

    @Test
    @DisplayName("예약 헤더가 없으면 실패를 반환하고 점유를 생성하지 않는다")
    void rejectsMissingReservationHeader() {
        var schedule = persistSchedule(10, 12);
        var missing = ReservationId.of(UUID.randomUUID());

        var status = adapter.reserve(missing, Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(missing)).isEmpty();
    }

    @Test
    @DisplayName("취소된 예약 헤더에는 점유를 생성하지 않는다")
    void rejectsCancelledReservationHeader() {
        var schedule = persistSchedule(10, 12);
        var reservation = saveReservation();
        reservation.delete(RETIRED_AT);
        reservationAdapter.save(reservation);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("일요일 예약은 ISO 요일 값이 일치하는 일요일 슬롯을 점유한다")
    void reservesSundaySchedule() {
        var sunday = SERVICE_DATE.minusDays(1);
        var schedule = persistSchedule(room.getId(), DayOfWeek.SUNDAY, 10, 12);
        var reservation = saveReservation(seat, sunday);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .singleElement()
                .satisfies(
                        allocation -> {
                            assertThat(allocation.getScheduleId()).isEqualTo(schedule.getId());
                            assertThat(allocation.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
                            assertThat(allocation.getReservationDate()).isEqualTo(sunday);
                        });
    }

    @Test
    @DisplayName("다른 좌석은 같은 날짜의 같은 슬롯을 예약할 수 있다")
    void reservesSameScheduleForDifferentSeat() {
        var schedule = persistSchedule(10, 12);
        persistAllocation(saveReservation(), schedule);
        var otherSeat = persistSeat();
        var reservation = saveReservation(otherSeat, SERVICE_DATE);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getSeatId)
                .containsExactly(otherSeat.getId());
    }

    @Test
    @DisplayName("같은 좌석도 다른 주의 같은 요일에는 같은 슬롯을 예약할 수 있다")
    void reservesSameScheduleOnDifferentDate() {
        var schedule = persistSchedule(10, 12);
        persistAllocation(saveReservation(), schedule);
        var nextWeek = SERVICE_DATE.plusWeeks(1);
        var reservation = saveReservation(seat, nextWeek);

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getReservationDate)
                .containsExactly(nextWeek);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("기존 점유의 시작 또는 종료 경계에 맞닿는 슬롯은 예약할 수 있다")
    void reservesAdjacentSchedule(boolean beforeOccupied) {
        var occupied = persistSchedule(10, 12);
        var adjacent = beforeOccupied ? persistSchedule(8, 10) : persistSchedule(12, 14);
        persistAllocation(saveReservation(), occupied);
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(adjacent.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(adjacent.getId());
    }

    @Test
    @DisplayName("서로 떨어진 슬롯을 예약해도 그 사이의 시간은 점유하지 않는다")
    void leavesGapBetweenSelectedSchedulesAvailable() {
        var morning = persistSchedule(10, 11);
        var gap = persistSchedule(12, 13);
        var afternoon = persistSchedule(14, 15);
        var first = saveReservation();
        assertThat(adapter.reserve(first.getId(), Set.of(morning.getId(), afternoon.getId())))
                .isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(gap.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId()))
                .extracting(ReservedSchedule::getScheduleId)
                .containsExactly(gap.getId());
    }

    @Test
    @DisplayName("취소된 예약의 점유는 같은 좌석의 재예약을 막지 않는다")
    void reservesScheduleAfterWholeReservationCancellation() {
        var schedule = persistSchedule(10, 12);
        var cancelled = saveReservation();
        var allocation = persistAllocation(cancelled, schedule);
        cancelled.delete(RETIRED_AT);
        reservationAdapter.save(cancelled);
        allocation.delete(RETIRED_AT);
        entityManager.flush();
        var reservation = saveReservation();

        var status = adapter.reserve(reservation.getId(), Set.of(schedule.getId()));

        assertThat(status).isEqualTo(ReserveScheduleStatus.ALL_SCHEDULE_SUCCESSFULLY_RESERVED);
        assertThat(allocations(reservation.getId())).hasSize(1);
    }

    @Test
    @DisplayName("요청 수보다 적게 점유되어 실패한 뒤 호출자가 롤백하면 헤더와 점유가 사라진다")
    void rollsBackHeaderAndAllocationsWithCallerTransaction() {
        var available = persistSchedule(10, 12);
        var reservation = saveReservation();
        var missing = ScheduleId.of(UUID.randomUUID());
        var status = adapter.reserve(reservation.getId(), Set.of(available.getId(), missing));
        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
        assertThat(allocations(reservation.getId())).hasSize(1);

        TestTransaction.flagForRollback();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(entityManager.find(Reservation.class, reservation.getId())).isNull();
        assertThat(allocations(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("점유 충돌로 실패한 뒤 호출자가 롤백하면 새 헤더와 일부 점유가 남지 않는다")
    void rollsBackAllNewWritesAfterOccupancyConflict() {
        var occupied = persistSchedule(10, 12);
        var available = persistSchedule(14, 16);
        persistAllocation(saveReservation(), occupied);
        var attempted = saveReservation();
        var status =
                adapter.reserve(attempted.getId(), Set.of(available.getId(), occupied.getId()));

        TestTransaction.flagForRollback();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(entityManager.find(Reservation.class, attempted.getId())).isNull();
        assertThat(allocations(attempted.getId())).isEmpty();
        assertThat(status).isEqualTo(ReserveScheduleStatus.FAILED);
    }

    @Test
    @DisplayName("헤더 플러시 예외 후 호출자가 롤백하면 잘못된 헤더와 점유가 남지 않는다")
    void rollsBackHeaderAfterFlushConstraintViolation() {
        var otherRoom = persistRoom();
        var schedule = persistSchedule(otherRoom.getId(), DayOfWeek.MONDAY, 10, 12);
        entityManager.flush();
        var invalid = reservation(otherRoom.getId(), seat.getId(), SERVICE_DATE);
        reservationAdapter.save(invalid);

        assertThatThrownBy(() -> adapter.reserve(invalid.getId(), Set.of(schedule.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);

        TestTransaction.flagForRollback();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(entityManager.find(Reservation.class, invalid.getId())).isNull();
        assertThat(allocations(invalid.getId())).isEmpty();
    }

    private ScheduleId unavailableSchedule(UnavailableSchedule reason) {
        return switch (reason) {
            case MISSING -> ScheduleId.of(UUID.randomUUID());
            case DELETED -> {
                var schedule = persistSchedule(14, 16);
                schedule.delete(RETIRED_AT);
                yield schedule.getId();
            }
            case OTHER_ROOM ->
                    persistSchedule(persistRoom().getId(), DayOfWeek.MONDAY, 14, 16).getId();
            case WRONG_WEEKDAY -> persistSchedule(room.getId(), DayOfWeek.TUESDAY, 14, 16).getId();
        };
    }

    private Room persistRoom() {
        var id = RoomId.of(UUID.randomUUID());
        return entityManager.persist(
                Room.of(
                        id,
                        "ROOM-" + id.getValue().toString().substring(0, 8),
                        "hall",
                        "test room",
                        SLOT_CREATED_AT));
    }

    private Seat persistSeat() {
        var id = SeatId.of(UUID.randomUUID());
        return entityManager.persist(
                Seat.of(
                        id,
                        room.getId(),
                        "SEAT-" + id.getValue().toString().substring(0, 8),
                        SLOT_CREATED_AT));
    }

    private Schedule persistSchedule(int startHour, int endHour) {
        return persistSchedule(room.getId(), DayOfWeek.MONDAY, startHour, endHour);
    }

    private Schedule persistSchedule(RoomId roomId, DayOfWeek day, int startHour, int endHour) {
        return entityManager.persist(
                Schedule.of(
                        UUID.randomUUID(),
                        roomId.getValue(),
                        day,
                        LocalTime.of(startHour, 0),
                        LocalTime.of(endHour, 0),
                        SLOT_CREATED_AT));
    }

    private Reservation saveReservation() {
        return saveReservation(seat, SERVICE_DATE);
    }

    private Reservation saveReservation(Seat targetSeat, LocalDate date) {
        var reservation = reservation(targetSeat.getRoomId(), targetSeat.getId(), date);
        reservationAdapter.save(reservation);
        return reservation;
    }

    private Reservation reservation(RoomId roomId, SeatId seatId, LocalDate date) {
        return Reservation.of(
                UUID.randomUUID(),
                roomId.getValue(),
                seatId.getValue(),
                date,
                TIME_ZONE,
                RESERVED_AT);
    }

    private ReservedSchedule persistAllocation(Reservation reservation, Schedule schedule) {
        var allocation =
                ReservedSchedule.of(
                        UUID.randomUUID(),
                        reservation.getId().getValue(),
                        reservation.getRoomId().getValue(),
                        reservation.getSeatId().getValue(),
                        schedule.getId().getValue(),
                        schedule.getStartAt(),
                        schedule.getEndAt(),
                        reservation.getReservationDate(),
                        reservation.getTimeZone(),
                        RESERVED_AT);
        entityManager.persist(allocation);
        entityManager.flush();
        return allocation;
    }

    private List<ReservedSchedule> allocations(ReservationId reservationId) {
        entityManager.clear();
        return entityManager
                .getEntityManager()
                .createQuery(
                        """
                        select allocation from ReservedSchedule allocation
                        where allocation.reservationId.value = :reservationId
                        order by allocation.startAt
                        """,
                        ReservedSchedule.class)
                .setParameter("reservationId", reservationId.getValue())
                .getResultList();
    }

    private enum UnavailableSchedule {
        MISSING,
        DELETED,
        OTHER_ROOM,
        WRONG_WEEKDAY
    }
}
