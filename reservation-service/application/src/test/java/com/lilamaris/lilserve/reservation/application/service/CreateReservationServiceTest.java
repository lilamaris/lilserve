package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.BlackoutScheduleFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveScheduleStatus;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatLockFindPort;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/** Verifies reservation behavior using locked resource reads and mocked persistence ports. */
@DisplayName("예약 생성 유스케이스")
@ExtendWith(MockitoExtension.class)
class CreateReservationServiceTest {
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    private static final SeatId SEAT_ID = SeatId.of(UUID.randomUUID());
    private static final UUID GENERATED_ID = UUID.randomUUID();
    private static final ReservationId RESERVATION_ID = ReservationId.of(GENERATED_ID);
    private static final ScheduleId MORNING = ScheduleId.of(UUID.randomUUID());
    private static final ScheduleId AFTERNOON = ScheduleId.of(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-11-05T23:30:00Z");
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 11, 9);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    @Mock private ReservationSavePort reservationSavePort;
    @Mock private ReserveSchedulePort reserveSchedulePort;
    @Mock private RoomLockFindPort roomLockFindPort;
    @Mock private SeatLockFindPort seatLockFindPort;
    @Mock private ScheduleLockFindPort scheduleLockFindPort;
    @Mock private BlackoutScheduleFindPort blackoutScheduleFindPort;

    private CreateReservationService service;

    @BeforeEach
    void setUp() {
        service = newService(CLOCK, () -> GENERATED_ID);
    }

    @ParameterizedTest
    @MethodSource("successfulRequests")
    @DisplayName("단건과 복수 슬롯 예약은 생성한 헤더와 시간대를 유지하고 요청 슬롯 전체를 전달한다")
    void savesHeaderAndReservesExactlyRequestedSchedules(
            Set<ScheduleId> schedules, String timezone) {
        givenResourcesLocked(schedules);
        when(reserveSchedulePort.reserve(RESERVATION_ID, schedules))
                .thenReturn(ReserveScheduleStatus.ALL_RESERVED);

        service = newService(Clock.fixed(NOW, ZoneId.of(timezone)), () -> GENERATED_ID);
        var result = service.create(command(schedules));

        var header = savedReservation();
        assertThat(header.getId()).isEqualTo(RESERVATION_ID);
        assertThat(header.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(header.getSeatId()).isEqualTo(SEAT_ID);
        assertThat(header.getReservationDate()).isEqualTo(SERVICE_DATE);
        assertThat(header.getTimeZone()).isEqualTo(timezone);
        assertThat(header.getCreatedAt()).isEqualTo(NOW);
        assertThat(header.getDeletedAt()).isNull();
        assertThat(result)
                .isEqualTo(
                        new CreateReservationResult(
                                RESERVATION_ID,
                                ROOM_ID,
                                SEAT_ID,
                                schedules,
                                SERVICE_DATE,
                                timezone,
                                NOW));
        verify(reserveSchedulePort).reserve(header.getId(), schedules);
    }

    @Test
    @DisplayName("방이 없으면 빈 잠금 결과를 예외로 바꾸고 저장을 시작하지 않는다")
    void rejectsMissingRoom() {
        when(roomLockFindPort.findByIdWithSharedLock(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(
                seatLockFindPort, scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("잠근 방이 삭제된 상태면 예약을 거부한다")
    void rejectsDeletedRoom() {
        var deleted = room();
        deleted.delete(NOW);
        when(roomLockFindPort.findByIdWithSharedLock(ROOM_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(
                seatLockFindPort, scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("좌석이 없으면 예약을 거부한다")
    void rejectsMissingSeat() {
        givenRoomLocked();
        when(seatLockFindPort.findByIdWithSharedLock(SEAT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("잠근 좌석이 삭제된 상태면 예약을 거부한다")
    void rejectsDeletedSeat() {
        givenRoomLocked();
        var deleted = seat(ROOM_ID);
        deleted.delete(NOW);
        when(seatLockFindPort.findByIdWithSharedLock(SEAT_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("좌석이 요청한 방에 속하지 않으면 예약을 거부한다")
    void rejectsSeatBelongingToAnotherRoom() {
        givenRoomLocked();
        when(seatLockFindPort.findByIdWithSharedLock(SEAT_ID))
                .thenReturn(Optional.of(seat(RoomId.of(UUID.randomUUID()))));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("요청한 슬롯 중 일부라도 없으면 예약을 거부한다")
    void rejectsMissingSchedule() {
        givenRoomLocked();
        givenSeatLocked();
        when(scheduleLockFindPort.findAllByIdsWithSharedLock(Set.of(MORNING, AFTERNOON)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING, AFTERNOON))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("잠금 조회 예외를 그대로 전파하고 예약 저장을 시작하지 않는다")
    void propagatesLockedReadFailure() {
        var failure = new IllegalStateException("Room lock query failed");
        when(roomLockFindPort.findByIdWithSharedLock(ROOM_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(
                seatLockFindPort, scheduleLockFindPort, reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("식별자 생성 예외를 그대로 전파하고 예약 저장을 시작하지 않는다")
    void propagatesIdGenerationFailure() {
        givenResourcesLocked(Set.of(MORNING));
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                newService(
                        CLOCK,
                        () -> {
                            throw failure;
                        });

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("헤더 저장 예외를 그대로 전파하고 슬롯 점유를 시도하지 않는다")
    void propagatesHeaderSaveFailureWithoutReserving() {
        givenResourcesLocked(Set.of(MORNING));
        var failure = new DataIntegrityViolationException("Header storage failed");
        doThrow(failure).when(reservationSavePort).save(any(Reservation.class));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reserveSchedulePort);
    }

    @ParameterizedTest
    @EnumSource(
            value = ReserveScheduleStatus.class,
            names = {"NONE_RESERVED", "PARTIAL_RESERVED"})
    @DisplayName("점유가 없거나 일부만 성공하면 예외로 전체 롤백을 요청한다")
    void rejectsIncompleteReservationStatus(ReserveScheduleStatus status) {
        givenResourcesLocked(Set.of(MORNING, AFTERNOON));
        when(reserveSchedulePort.reserve(RESERVATION_ID, Set.of(MORNING, AFTERNOON)))
                .thenReturn(status);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING, AFTERNOON))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to reserve schedules.");
    }

    @Test
    @DisplayName("점유 중 발생한 무결성 예외를 그대로 전파한다")
    void propagatesReservationIntegrityFailure() {
        givenResourcesLocked(Set.of(MORNING));
        var failure = new DataIntegrityViolationException("Allocation constraint violated");
        when(reserveSchedulePort.reserve(RESERVATION_ID, Set.of(MORNING))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);
    }

    private void givenResourcesLocked(Set<ScheduleId> schedules) {
        givenRoomLocked();
        givenSeatLocked();
        when(scheduleLockFindPort.findAllByIdsWithSharedLock(schedules))
                .thenReturn(Optional.of(schedules));
        when(blackoutScheduleFindPort.existsActiveByScheduleIdsAt(schedules, NOW))
                .thenReturn(false);
    }

    private void givenRoomLocked() {
        when(roomLockFindPort.findByIdWithSharedLock(ROOM_ID)).thenReturn(Optional.of(room()));
    }

    private void givenSeatLocked() {
        when(seatLockFindPort.findByIdWithSharedLock(SEAT_ID))
                .thenReturn(Optional.of(seat(ROOM_ID)));
    }

    private CreateReservationService newService(
            Clock clock, java.util.function.Supplier<UUID> ids) {
        return new CreateReservationService(
                reservationSavePort,
                reserveSchedulePort,
                roomLockFindPort,
                seatLockFindPort,
                scheduleLockFindPort,
                blackoutScheduleFindPort,
                ids::get,
                clock);
    }

    private static Room room() {
        return Room.of(ROOM_ID, "ROOM-01", "hall", "test room", NOW);
    }

    private static Seat seat(RoomId roomId) {
        return Seat.of(SEAT_ID, roomId, "SEAT-01", NOW);
    }

    private Reservation savedReservation() {
        var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationSavePort).save(captor.capture());
        return captor.getValue();
    }

    private CreateReservationCommand command(Set<ScheduleId> schedules) {
        return CreateReservationCommand.of(ROOM_ID, SEAT_ID, schedules, SERVICE_DATE);
    }

    private static Stream<Arguments> successfulRequests() {
        return Stream.of(
                arguments(Set.of(MORNING), "Asia/Seoul"),
                arguments(Set.of(MORNING, AFTERNOON), "Asia/Seoul"),
                arguments(Set.of(MORNING), "UTC"),
                arguments(Set.of(MORNING, AFTERNOON), "UTC"));
    }
}
