package com.lilamaris.lilserve.reservation.web;

import com.lilamaris.lilserve.reservation.web.controller.RoomController;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps room application failures to HTTP status codes and RFC 9457 problem details.
 *
 * <p>This advice applies to {@link RoomController}. Spring MVC handles request body validation
 * failures before the use case runs.
 */
@RestControllerAdvice(assignableTypes = RoomController.class)
public class RoomExceptionHandler {

    /**
     * Maps an unavailable room to a 404 problem detail: it does not exist, or it is not in the
     * deletion state the operation requires.
     *
     * @param exception the missing target
     * @return the 404 problem detail
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleMissingRoom(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "room does not exist.");
    }

    /**
     * Maps persistence integrity violations to a 409 problem detail.
     *
     * @param exception the integrity violation raised by persistence
     * @return the 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateCode(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "room code is already used.");
    }

    /**
     * Maps rejected application values to a 400 problem detail.
     *
     * @param exception the rejected value, whose message is the detail
     * @return the 400 problem detail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidValue(IllegalArgumentException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                Objects.requireNonNullElse(exception.getMessage(), "invalid request."));
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
