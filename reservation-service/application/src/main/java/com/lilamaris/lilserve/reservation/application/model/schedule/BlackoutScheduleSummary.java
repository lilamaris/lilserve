package com.lilamaris.lilserve.reservation.application.model.schedule;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;

/**
 * The part of a reservation intake blackout a slot list reports.
 *
 * @param blackoutScheduleId the blackout identifier
 * @param validPeriod the half-open interval the blackout blocks intake for
 * @param reason the blackout reason
 */
public record BlackoutScheduleSummary(
        BlackoutScheduleId blackoutScheduleId, InstantRange validPeriod, String reason) {
    /**
     * Projects a reservation intake blackout, preserving its half-open validity interval.
     *
     * @param blackoutSchedule the blackout to summarize
     * @return its identifier, interval, and reason
     */
    public static BlackoutScheduleSummary from(BlackoutSchedule blackoutSchedule) {
        return new BlackoutScheduleSummary(
                blackoutSchedule.getId(),
                InstantRange.of(blackoutSchedule.getStartsAt(), blackoutSchedule.getEndsAt()),
                blackoutSchedule.getReason());
    }
}
