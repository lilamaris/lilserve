package com.lilamaris.lilserve.reservation.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilserve.reservation.application.model.time.InstantRange;
import com.lilamaris.lilserve.reservation.application.port.in.CreateBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutSchedulePeriodUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutScheduleReasonUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteBlackoutScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutSchedulePeriodCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateBlackoutScheduleReasonCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateBlackoutScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateBlackoutScheduleResult;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.web.controller.BlackoutScheduleController;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("임시 차단 웹 API")
@WebMvcTest(controllers = BlackoutScheduleController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({
    BlackoutScheduleController.class,
    BlackoutScheduleExceptionHandler.class,
    ApiVersionConfiguration.class
})
class BlackoutScheduleControllerTest {
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SCHEDULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID BLACKOUT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T01:00:00Z");
    private static final Instant STARTS_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-01-03T00:00:00Z");
    private static final Instant REPLACEMENT_STARTS_AT = Instant.parse("2026-01-04T00:00:00Z");
    private static final Instant REPLACEMENT_ENDS_AT = Instant.parse("2026-01-05T00:00:00Z");
    private static final String REASON = "facility maintenance";
    private static final String REPLACEMENT_REASON = "moved to next week";
    private static final String BLACKOUTS_URL = "/api/v1/blackouts";
    private static final String BLACKOUT_URL = BLACKOUTS_URL + "/" + BLACKOUT_ID;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateBlackoutScheduleUseCase createBlackoutScheduleUseCase;
    @MockitoBean private UpdateBlackoutSchedulePeriodUseCase updateBlackoutSchedulePeriodUseCase;
    @MockitoBean private UpdateBlackoutScheduleReasonUseCase updateBlackoutScheduleReasonUseCase;
    @MockitoBean private DeleteBlackoutScheduleUseCase deleteBlackoutScheduleUseCase;

    @Test
    @DisplayName("생성 요청은 생성된 차단 정보를 201 본문으로 반환한다")
    void createsBlackoutAndReturnsCreatedBlackout() throws Exception {
        var body = createBody();
        when(createBlackoutScheduleUseCase.create(createCommand()))
                .thenReturn(CreateBlackoutScheduleResult.from(createdBlackout()));

        mockMvc.perform(post(BLACKOUTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blackoutScheduleId").value(BLACKOUT_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$.reason").value(REASON))
                .andExpect(jsonPath("$.startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.endsAt").value(ENDS_AT.toString()))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("빈 이유를 담고 생성 요청은 차단하지 않고 거부한다")
    void rejectsCreateRequestWithBlankReason() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","scheduleId":"00000000-0000-0000-\
                0000-000000000003","startsAt":"2026-01-02T00:00:00Z","endsAt":"2026-01-03T00:00:00Z",\
                "reason":""}
                """;

        mockMvc.perform(post(BLACKOUTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createBlackoutScheduleUseCase);
    }

    @Test
    @DisplayName("이유가 상한을 넘는 생성 요청은 차단하지 않고 거부한다")
    void rejectsCreateRequestWithReasonAboveLengthLimit() throws Exception {
        var reason = "b".repeat(BlackoutSchedule.MAX_REASON_LENGTH + 1);
        var body =
                "{\"roomId\":\"00000000-0000-0000-0000-000000000001\",\"scheduleId\":\"00000000-0000"
                        + "-0000-0000-000000000003\",\"startsAt\":\"2026-01-02T00:00:00Z\","
                        + "\"endsAt\":\"2026-01-03T00:00:00Z\",\"reason\":\""
                        + reason
                        + "\"}";

        mockMvc.perform(post(BLACKOUTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createBlackoutScheduleUseCase);
    }

    @Test
    @DisplayName("종료가 시작과 같은 생성 요청은 차단하지 않고 거부한다")
    void rejectsCreateRequestWithIntervalEndingAtStart() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","scheduleId":"00000000-0000-0000-\
                0000-000000000003","startsAt":"2026-01-03T00:00:00Z","endsAt":"2026-01-03T00:00:00Z",\
                "reason":"facility maintenance"}
                """;

        mockMvc.perform(post(BLACKOUTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("must be after 2026-01-03T00:00:00Z."));

        verifyNoInteractions(createBlackoutScheduleUseCase);
    }

    @Test
    @DisplayName("존재하지 않는 스케줄에 대한 생성은 404 problem detail로 답한다")
    void returnsNotFoundWhenScheduleIsMissing() throws Exception {
        doThrow(new NoSuchElementException("schedule does not exists."))
                .when(createBlackoutScheduleUseCase)
                .create(createCommand());

        mockMvc.perform(
                        post(BLACKOUTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("blackout schedule does not exist."));
    }

    @Test
    @DisplayName("룸에 속하지 않은 스케줄에 대한 생성은 409 problem detail로 답한다")
    void returnsConflictWhenScheduleBelongsToAnotherRoom() throws Exception {
        doThrow(new DataIntegrityViolationException("fk_blackout_schedule_room"))
                .when(createBlackoutScheduleUseCase)
                .create(createCommand());

        mockMvc.perform(
                        post(BLACKOUTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("schedule does not belong to the room."));
    }

    @Test
    @DisplayName("차단 기간을 변경하고 변경된 차단 정보를 200 본문으로 반환한다")
    void replacesPeriodAndReturnsChangedBlackout() throws Exception {
        var body =
                """
                {"startsAt":"2026-01-04T00:00:00Z","endsAt":"2026-01-05T00:00:00Z"}
                """;
        when(updateBlackoutSchedulePeriodUseCase.updateRange(periodCommand()))
                .thenReturn(UpdateBlackoutScheduleResult.from(changedPeriodBlackout()));

        mockMvc.perform(
                        patch(BLACKOUT_URL + "/period")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blackoutScheduleId").value(BLACKOUT_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$.startsAt").value(REPLACEMENT_STARTS_AT.toString()))
                .andExpect(jsonPath("$.endsAt").value(REPLACEMENT_ENDS_AT.toString()))
                .andExpect(jsonPath("$.reason").value(REASON))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("종료가 시작과 같은 기간 변경은 차단 기간을 변경하지 않고 거부한다")
    void rejectsPeriodEndingAtStart() throws Exception {
        var body =
                """
                {"startsAt":"2026-01-05T00:00:00Z","endsAt":"2026-01-05T00:00:00Z"}
                """;

        mockMvc.perform(
                        patch(BLACKOUT_URL + "/period")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("must be after 2026-01-05T00:00:00Z."));

        verifyNoInteractions(updateBlackoutSchedulePeriodUseCase);
    }

    @Test
    @DisplayName("삭제된 차단의 기간 변경은 404 problem detail로 답한다")
    void returnsNotFoundWhenPeriodTargetIsDeleted() throws Exception {
        var body =
                """
                {"startsAt":"2026-01-04T00:00:00Z","endsAt":"2026-01-05T00:00:00Z"}
                """;
        doThrow(new NoSuchElementException("blackout schedule does not exists."))
                .when(updateBlackoutSchedulePeriodUseCase)
                .updateRange(periodCommand());

        mockMvc.perform(
                        patch(BLACKOUT_URL + "/period")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("blackout schedule does not exist."));
    }

    @Test
    @DisplayName("차단 사유를 변경하고 변경된 차단 정보를 200 본문으로 반환한다")
    void replacesReasonAndReturnsChangedBlackout() throws Exception {
        var body = "{\"reason\":\"moved to next week\"}";
        when(updateBlackoutScheduleReasonUseCase.updateReason(reasonCommand()))
                .thenReturn(UpdateBlackoutScheduleResult.from(changedReasonBlackout()));

        mockMvc.perform(
                        patch(BLACKOUT_URL + "/reason")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blackoutScheduleId").value(BLACKOUT_ID.toString()))
                .andExpect(jsonPath("$.reason").value(REPLACEMENT_REASON))
                .andExpect(jsonPath("$.startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.endsAt").value(ENDS_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("빈 이유를 담고 사유 변경은 차단을 변경하지 않고 거부한다")
    void rejectsReasonChangeWithBlankReason() throws Exception {
        var body = "{\"reason\":\"\"}";

        mockMvc.perform(
                        patch(BLACKOUT_URL + "/reason")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateBlackoutScheduleReasonUseCase);
    }

    @Test
    @DisplayName("삭제 요청은 대문을 두고도 본문 없이 204로 답한다")
    void deletesBlackoutWithoutResponseBody() throws Exception {
        mockMvc.perform(delete(BLACKOUT_URL))
                .andExpect(status().isNoContent())
                .andExpect(content().bytes(new byte[0]));

        verify(deleteBlackoutScheduleUseCase)
                .delete(DeleteBlackoutScheduleCommand.of(BlackoutScheduleId.of(BLACKOUT_ID)));
    }

    @Test
    @DisplayName("이미 삭제된 차단의 삭제는 404 problem detail로 답한다")
    void returnsNotFoundWhenDeleteTargetIsDeleted() throws Exception {
        doThrow(new NoSuchElementException("blackout schedule does not exists."))
                .when(deleteBlackoutScheduleUseCase)
                .delete(DeleteBlackoutScheduleCommand.of(BlackoutScheduleId.of(BLACKOUT_ID)));

        mockMvc.perform(delete(BLACKOUT_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("blackout schedule does not exist."));
    }

    @Test
    @DisplayName("식별자 형태가 맞지 않으면 400으로 거부한다")
    void rejectsMalformedBlackoutId() throws Exception {
        mockMvc.perform(delete(BLACKOUTS_URL + "/not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(deleteBlackoutScheduleUseCase);
    }

    private String createBody() {
        return """
            {"roomId":"00000000-0000-0000-0000-000000000001","scheduleId":"00000000-0000-0000-\
            0000-000000000003","startsAt":"2026-01-02T00:00:00Z","endsAt":"2026-01-03T00:00:00Z",\
            "reason":"facility maintenance"}
            """;
    }

    private CreateBlackoutScheduleCommand createCommand() {
        return CreateBlackoutScheduleCommand.of(
                RoomId.of(ROOM_ID),
                ScheduleId.of(SCHEDULE_ID),
                InstantRange.of(STARTS_AT, ENDS_AT),
                REASON);
    }

    private UpdateBlackoutSchedulePeriodCommand periodCommand() {
        return UpdateBlackoutSchedulePeriodCommand.of(
                BlackoutScheduleId.of(BLACKOUT_ID),
                InstantRange.of(REPLACEMENT_STARTS_AT, REPLACEMENT_ENDS_AT));
    }

    private UpdateBlackoutScheduleReasonCommand reasonCommand() {
        return UpdateBlackoutScheduleReasonCommand.of(
                BlackoutScheduleId.of(BLACKOUT_ID), REPLACEMENT_REASON);
    }

    private BlackoutSchedule createdBlackout() {
        return BlackoutSchedule.of(
                BlackoutScheduleId.of(BLACKOUT_ID),
                RoomId.of(ROOM_ID),
                ScheduleId.of(SCHEDULE_ID),
                REASON,
                STARTS_AT,
                ENDS_AT,
                CREATED_AT);
    }

    private BlackoutSchedule changedPeriodBlackout() {
        var blackout = createdBlackout();
        blackout.updatePeriod(REPLACEMENT_STARTS_AT, REPLACEMENT_ENDS_AT, UPDATED_AT);
        return blackout;
    }

    private BlackoutSchedule changedReasonBlackout() {
        var blackout = createdBlackout();
        blackout.updateReason(REPLACEMENT_REASON, UPDATED_AT);
        return blackout;
    }
}
