package com.lilamaris.lilserve.reservation.web;

import com.lilamaris.lilserve.reservation.web.controller.ReservationController;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps reservation creation failures to HTTP problem details. */
@RestControllerAdvice(assignableTypes = ReservationController.class)
public class ReservationExceptionHandler {
    /**
     * Reports missing or deleted reservation resources.
     *
     * @param exception the missing resource failure
     * @return a 404 problem detail
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleMissingResource(NoSuchElementException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "room, seat or schedule does not exist.");
    }

    /**
     * Reports invalid application values, including a seat from another room.
     *
     * @param exception the rejected value
     * @return a 400 problem detail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidValue(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                Objects.requireNonNullElse(exception.getMessage(), "invalid request."));
    }

    /**
     * Reports usage blackouts and incomplete slot allocations.
     *
     * @param exception the rejected reservation state
     * @return a 409 problem detail
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleUnavailableReservation(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                Objects.requireNonNullElse(
                        exception.getMessage(), "reservation cannot be created."));
    }

    /**
     * Reports persistence constraints without exposing database details.
     *
     * @param exception the persistence conflict
     * @return a 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityConflict(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "reservation conflicts with existing data.");
    }
}
