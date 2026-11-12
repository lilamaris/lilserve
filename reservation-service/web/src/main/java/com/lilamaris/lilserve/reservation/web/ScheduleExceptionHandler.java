package com.lilamaris.lilserve.reservation.web;

import com.lilamaris.lilserve.reservation.web.controller.ScheduleController;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps schedule application failures to HTTP status codes and RFC 9457 problem details.
 *
 * <p>This advice applies to {@link ScheduleController}. Spring MVC handles request body validation
 * failures before the use case runs.
 */
@RestControllerAdvice(assignableTypes = ScheduleController.class)
public class ScheduleExceptionHandler {

    /**
     * Maps an unavailable slot to a 404 problem detail: it does not exist, or it is already
     * retired. The parent room of a new slot is reported here too, as 404 is the answer for a
     * target that is not there.
     *
     * @param exception the missing target
     * @return the 404 problem detail
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleMissingSchedule(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "schedule does not exist.");
    }

    /**
     * Maps persistence integrity violations to a 409 problem detail. The schema keeps active slots
     * of one room apart, by excluding overlapping intervals and by the unique active slot per room,
     * weekday, and start time, so a request that asks for a slot the room already offers is
     * answered here.
     *
     * @param exception the integrity violation raised by persistence
     * @return the 409 problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleOverlappingSchedule(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "schedule overlaps a slot the room already offers.");
    }

    /**
     * Maps rejected application values to a 400 problem detail. A time range whose end is not after
     * its start is reported here, as the range validates its own bounds when the command builds it.
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
