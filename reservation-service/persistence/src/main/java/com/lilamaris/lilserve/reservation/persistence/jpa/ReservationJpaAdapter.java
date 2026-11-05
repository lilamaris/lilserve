package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ReservationFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.domain.id.ReservationId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ReservationRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationJpaAdapter implements ReservationSavePort, ReservationFindPort {
    private final ReservationRepository repository;

    /** {@inheritDoc} */
    @Override
    public void save(Reservation reservation) {
        repository.save(reservation);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Reservation> findNotStartedById(ReservationId reservationId, Instant now) {
        return repository.findNotStartedById(reservationId, now);
    }
}
