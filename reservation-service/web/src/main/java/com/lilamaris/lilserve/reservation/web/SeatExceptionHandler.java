package com.lilamaris.lilserve.reservation.web;

import com.lilamaris.lilserve.reservation.web.controller.SeatController;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps seat failures to HTTP status codes, as RFC 9457 problem details. The use cases report a
 * missing seat, a seat in the wrong deletion state, and a duplicate code, and none of those is the
 * web layer's decision to make, so each is translated here.
 *
 * <p>The advice is scoped to {@link SeatController}, so the room advice keeps answering room
 * failures with the room wording.
 *
 * <p>Request bodies that fail Bean Validation are answered with 400 by Spring MVC itself, so this
 * advice handles only the failures that come out of the application layer.
 */
@RestControllerAdvice(assignableTypes = SeatController.class)
public class SeatExceptionHandler {

    /**
     * Reports a seat the operation can not act on as 404: it does not exist, or it is not in the
     * deletion state the operation requires. The parent room of a new seat is reported here too, as
     * 404 is the answer for a target that is not there.
     *
     * @param exception the missing target
     * @return the 404 problem detail
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleMissingSeat(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "seat does not exist.");
    }

    /**
     * Reports a code that is already used by another seat as 409, the database uniqueness rule.
     *
     * @param exception the integrity violation raised by persistence
     * @return the 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateCode(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "seat code is already used.");
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
