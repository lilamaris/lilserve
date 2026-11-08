package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.DeleteReservationResult;
import com.lilamaris.lilserve.reservation.application.port.out.ReleaseSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReleaseScheduleStatus;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("예약 취소 유스케이스")
@ExtendWith(MockitoExtension.class)
class DeleteReservationServiceTest {
    private static final ReservationId RESERVATION_ID = ReservationId.of(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");

    @Mock private ReservationFindPort reservationFindPort;
    @Mock private ReservationSavePort reservationSavePort;
    @Mock private ReleaseSchedulePort releaseSchedulePort;

    private DeleteReservationService service;

    @BeforeEach
    void setUp() {
        service =
                new DeleteReservationService(
                        reservationFindPort,
                        reservationSavePort,
                        releaseSchedulePort,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("헤더와 점유를 같은 시각으로 취소하고 예약 식별자와 취소 시각을 반환한다")
    void returnsCancelledReservationAfterReleasingAllocations() {
        var reservation = reservation();
        when(reservationFindPort.findNotStartedById(RESERVATION_ID, NOW))
                .thenReturn(Optional.of(reservation));
        when(releaseSchedulePort.release(RESERVATION_ID, NOW))
                .thenReturn(ReleaseScheduleStatus.ALL_RELEASED);

        var result = service.delete(DeleteReservationCommand.of(RESERVATION_ID));

        assertThat(result).isEqualTo(new DeleteReservationResult(RESERVATION_ID, NOW));
        assertThat(reservation.getDeletedAt()).isEqualTo(result.deletedAt());
        verify(reservationSavePort).save(reservation);
        verify(releaseSchedulePort).release(result.reservationId(), result.deletedAt());
    }

    @Test
    @DisplayName("취소 가능한 예약이 없으면 저장과 점유 해제를 수행하지 않는다")
    void rejectsReservationThatCannotBeCancelled() {
        when(reservationFindPort.findNotStartedById(RESERVATION_ID, NOW))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(DeleteReservationCommand.of(RESERVATION_ID)))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(reservationSavePort, releaseSchedulePort);
    }

    @Test
    @DisplayName("점유 해제가 실패하면 성공 결과 대신 예외를 그대로 전파한다")
    void propagatesReleaseFailureInsteadOfReturningResult() {
        when(reservationFindPort.findNotStartedById(RESERVATION_ID, NOW))
                .thenReturn(Optional.of(reservation()));
        var failure = new IllegalStateException("Failed to release allocations");
        doThrow(failure).when(releaseSchedulePort).release(RESERVATION_ID, NOW);

        assertThatThrownBy(() -> service.delete(DeleteReservationCommand.of(RESERVATION_ID)))
                .isSameAs(failure);
    }

    private Reservation reservation() {
        return Reservation.of(
                RESERVATION_ID,
                RoomId.of(UUID.randomUUID()),
                SeatId.of(UUID.randomUUID()),
                LocalDate.of(2026, 11, 9),
                "Asia/Seoul",
                NOW.minusSeconds(60));
    }
}
