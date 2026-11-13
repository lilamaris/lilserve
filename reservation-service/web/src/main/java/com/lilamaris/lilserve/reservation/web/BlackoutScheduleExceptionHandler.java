package com.lilamaris.lilserve.reservation.web;

import com.lilamaris.lilserve.reservation.web.controller.BlackoutScheduleController;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps blackout application failures to HTTP status codes and RFC 9457 problem details.
 *
 * <p>This advice applies to {@link BlackoutScheduleController}. Spring MVC handles request body
 * validation failures before the use case runs.
 */
@RestControllerAdvice(assignableTypes = BlackoutScheduleController.class)
public class BlackoutScheduleExceptionHandler {

    /**
     * Maps an unavailable blackout to a 404 problem detail: it does not exist, or it is already
     * deleted. The parent room and slot of a new blackout are reported here too, as 404 is the
     * answer for a target that is not there.
     *
     * @param exception the missing target
     * @return the 404 problem detail
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleMissingBlackout(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "blackout schedule does not exist.");
    }

    /**
     * Maps persistence integrity violations to a 409 problem detail. The schema requires that the
     * slot a blackout blocks belongs to the room the request carries, so a pair that does not
     * belong together is answered here. Overlapping blackouts are allowed, so overlap is not a
     * conflict.
     *
     * @param exception the integrity violation raised by persistence
     * @return the 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleForeignSchedule(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "schedule does not belong to the room.");
    }

    /**
     * Maps rejected application values to a 400 problem detail. An usage interval whose end is not
     * after its start is reported here, as the interval validates its own bounds when the command
     * builds it.
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
