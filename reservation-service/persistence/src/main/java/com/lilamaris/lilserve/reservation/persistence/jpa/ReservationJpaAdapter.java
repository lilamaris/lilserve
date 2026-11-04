package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.domain.Reservation;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationJpaAdapter implements ReservationSavePort {
    private final ReservationRepository repository;

    /** {@inheritDoc} */
    @Override
    public void save(Reservation reservation) {
        repository.save(reservation);
    }
}
