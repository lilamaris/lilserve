package com.lilamaris.lilserve.reservation.application.model.schedule;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;

/**
 * The part of a room usage blackout a slot list reports.
 *
 * @param blackoutScheduleId the blackout identifier
 * @param validPeriod the half-open interval the blackout blocks usage for
 * @param reason the blackout reason
 */
public record BlackoutScheduleSummary(
        BlackoutScheduleId blackoutScheduleId, InstantRange validPeriod, String reason) {
    /**
     * Projects a room usage blackout, preserving its half-open validity interval.
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
