package com.lilamaris.lilserve.reservation.application.service;

import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Implements {@link FindRoomUseCase} by loading a non-deleted room through {@link RoomFindPort} in
 * a read-only transaction.
 */
@Service
@Validated
@RequiredArgsConstructor
public class FindRoomService implements FindRoomUseCase {
    private final RoomFindPort roomFindPort;

    /**
     * {@inheritDoc}
     *
     * <p>The room is read through the not-deleted lookup, so a deleted room is a missing room here.
     */
    @Override
    @Transactional(readOnly = true)
    public FindRoomResult find(FindRoomQuery query) {
        return roomFindPort
                .findNotDeletedById(query.roomId())
                .map(FindRoomResult::from)
                .orElseThrow(() -> new NoSuchElementException("room does not exists."));
    }
}
