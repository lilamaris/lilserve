package com.lilamaris.lilserve.reservation.persistence.jpa;

import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import com.lilamaris.lilserve.reservation.persistence.jpa.repository.SeatRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements seat persistence ports using Spring Data JPA within the caller's transaction. */
@Component
@RequiredArgsConstructor
public class SeatJpaAdapter implements SeatFindPort, SeatSavePort, SeatLockFindPort {
    private final SeatRepository repository;

    /** {@inheritDoc} */
    @Override
    public Optional<Seat> findById(SeatId seatId) {
        return repository.findById(seatId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Seat> findNotDeletedById(SeatId seatId) {
        return repository.findById(seatId).filter(seat -> !seat.isDeleted());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Seat> findDeletedById(SeatId seatId) {
        return repository.findById(seatId).filter(Seat::isDeleted);
    }

    /** {@inheritDoc} */
    @Override
    public void save(Seat seat) {
        repository.save(seat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lock and the read are requested by one query, so the returned seat is the row this
     * transaction protects. A missing row is reported as an empty result.
     */
    @Override
    public Optional<Seat> findLockedById(SeatId seatId) {
        return repository.findByIdWithSharedLock(seatId);
    }
}
