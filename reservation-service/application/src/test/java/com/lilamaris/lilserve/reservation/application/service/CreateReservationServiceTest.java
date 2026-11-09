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
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveScheduleStatus;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
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
    @Mock private RoomFindPort roomFindPort;
    @Mock private SeatFindPort seatFindPort;

    private CreateReservationService service;

    @BeforeEach
    void setUp() {
        service =
                new CreateReservationService(
                        reservationSavePort,
                        reserveSchedulePort,
                        roomFindPort,
                        seatFindPort,
                        () -> GENERATED_ID,
                        CLOCK);
    }

    @ParameterizedTest
    @MethodSource("successfulRequests")
    @DisplayName("단건과 복수 슬롯 예약은 생성한 헤더의 범위와 설정 시간대를 유지하고 요청 슬롯 전체를 전달한다")
    void savesHeaderAndReservesExactlyRequestedSchedules(
            Set<ScheduleId> schedules, String timezone) {
        givenRoomFound();
        givenSeatFound(ROOM_ID);
        service =
                new CreateReservationService(
                        reservationSavePort,
                        reserveSchedulePort,
                        roomFindPort,
                        seatFindPort,
                        () -> GENERATED_ID,
                        Clock.fixed(NOW, ZoneId.of(timezone)));
        when(reserveSchedulePort.reserve(RESERVATION_ID, schedules))
                .thenReturn(ReserveScheduleStatus.ALL_RESERVED);

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
    @DisplayName("사용 가능한 룸이 없으면 예약 헤더와 점유를 저장하지 않는다")
    void rejectsMissingOrDeletedRoom() {
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("사용 가능한 좌석이 없으면 예약 헤더와 점유를 저장하지 않는다")
    void rejectsMissingOrDeletedSeat() {
        givenRoomFound();
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("좌석이 요청 룸에 속하지 않으면 예약 헤더와 점유를 저장하지 않는다")
    void rejectsSeatBelongingToAnotherRoom() {
        givenRoomFound();
        givenSeatFound(RoomId.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("룸 조회 예외를 그대로 전파하고 예약 헤더와 점유를 저장하지 않는다")
    void propagatesRoomLookupFailure() {
        var failure = new IllegalStateException("Room lookup failed");
        when(roomFindPort.findNotDeletedById(ROOM_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("좌석 조회 예외를 그대로 전파하고 예약 헤더와 점유를 저장하지 않는다")
    void propagatesSeatLookupFailure() {
        givenRoomFound();
        var failure = new IllegalStateException("Seat lookup failed");
        when(seatFindPort.findNotDeletedById(SEAT_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("식별자 생성 예외를 그대로 전파하고 예약 헤더와 점유를 저장하지 않는다")
    void propagatesIdGenerationFailure() {
        givenRoomFound();
        givenSeatFound(ROOM_ID);
        var failure = new IllegalStateException("Identifier generation failed");
        service =
                new CreateReservationService(
                        reservationSavePort,
                        reserveSchedulePort,
                        roomFindPort,
                        seatFindPort,
                        () -> {
                            throw failure;
                        },
                        CLOCK);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reservationSavePort, reserveSchedulePort);
    }

    @Test
    @DisplayName("헤더 저장 예외를 그대로 전파하고 슬롯 점유를 시도하지 않는다")
    void propagatesHeaderSaveFailureWithoutReserving() {
        givenRoomFound();
        givenSeatFound(ROOM_ID);
        var failure = new DataIntegrityViolationException("Header storage failed");
        doThrow(failure).when(reservationSavePort).save(any(Reservation.class));

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);

        verifyNoInteractions(reserveSchedulePort);
    }

    @ParameterizedTest
    @EnumSource(
            value = ReserveScheduleStatus.class,
            names = {"NONE_RESERVED", "PARTIAL_RESERVED"})
    @DisplayName("점유 포트가 미점유 또는 부분 점유를 반환하면 호출자에게 예외를 전파한다")
    void rejectsIncompleteReservationStatus(ReserveScheduleStatus status) {
        givenRoomFound();
        givenSeatFound(ROOM_ID);
        when(reserveSchedulePort.reserve(RESERVATION_ID, Set.of(MORNING, AFTERNOON)))
                .thenReturn(status);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING, AFTERNOON))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to reserve schedules.");
    }

    @Test
    @DisplayName("점유 중 발생한 무결성 예외를 다른 실패로 바꾸지 않고 그대로 전파한다")
    void propagatesReservationIntegrityFailure() {
        givenRoomFound();
        givenSeatFound(ROOM_ID);
        var failure = new DataIntegrityViolationException("Allocation constraint violated");
        when(reserveSchedulePort.reserve(RESERVATION_ID, Set.of(MORNING))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(command(Set.of(MORNING)))).isSameAs(failure);
    }

    private void givenRoomFound() {
        when(roomFindPort.findNotDeletedById(ROOM_ID))
                .thenReturn(Optional.of(Room.of(ROOM_ID, "ROOM-01", "hall", "test room", NOW)));
    }

    private void givenSeatFound(RoomId roomId) {
        when(seatFindPort.findNotDeletedById(SEAT_ID))
                .thenReturn(Optional.of(Seat.of(SEAT_ID, roomId, "SEAT-01", NOW)));
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
