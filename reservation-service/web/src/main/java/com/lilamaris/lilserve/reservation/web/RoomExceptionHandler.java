package com.lilamaris.lilserve.reservation.web;

import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps room failures to HTTP status codes, as RFC 9457 problem details. The use cases report a
 * missing room, a room in the wrong deletion state, and a duplicate code, and none of those is the
 * web layer's decision to make, so each is translated here.
 *
 * <p>Request bodies that fail Bean Validation are answered with 400 by Spring MVC itself, so this
 * advice handles only the failures that come out of the application layer.
 */
@RestControllerAdvice
public class RoomExceptionHandler {

    /**
     * Reports a room the operation can not act on as 404: it does not exist, or it is not in the
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
     * Reports a code that is already used by another room as 409, the database uniqueness rule.
     *
     * @param exception the integrity violation raised by persistence
     * @return the 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateCode(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "room code is already used.");
    }

    /**
     * Reports a value the domain rejected, including a malformed identifier, as 400.
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
