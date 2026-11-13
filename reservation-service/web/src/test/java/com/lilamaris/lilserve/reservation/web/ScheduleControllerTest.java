package com.lilamaris.lilserve.reservation.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilserve.reservation.application.model.schedule.DaySchedule;
import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListScheduleQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSchedulesResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.ListScheduleResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateScheduleResult;
import com.lilamaris.lilserve.reservation.domain.BlackoutSchedule;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.BlackoutScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.web.controller.ScheduleController;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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

@DisplayName("스케줄 웹 API")
@WebMvcTest(controllers = ScheduleController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({ScheduleController.class, ScheduleExceptionHandler.class, ApiVersionConfiguration.class})
class ScheduleControllerTest {
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SCHEDULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID REPLACEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID BLACKOUT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant BLACKOUT_END = Instant.parse("2026-01-03T00:00:00Z");
    private static final LocalTime START_AT = LocalTime.parse("09:00");
    private static final LocalTime END_AT = LocalTime.parse("11:00");
    private static final String SCHEDULES_URL = "/api/v1/schedules";
    private static final String SCHEDULE_URL = SCHEDULES_URL + "/" + SCHEDULE_ID;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateScheduleUseCase createScheduleUseCase;
    @MockitoBean private ListScheduleUseCase listScheduleUseCase;
    @MockitoBean private UpdateScheduleUseCase updateScheduleUseCase;
    @MockitoBean private DeleteScheduleUseCase deleteScheduleUseCase;

    @Test
    @DisplayName("요청 본문으로 스케줄 하나를 생성하고 생성된 스케줄을 201로 반환한다")
    void createsSlotAndReturnsCreatedSlot() throws Exception {
        var body = createBody();
        when(createScheduleUseCase.create(oneSlotCommand()))
                .thenReturn(CreateScheduleResult.from(slot()));

        mockMvc.perform(post(SCHEDULES_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startAt").value("09:00:00"))
                .andExpect(jsonPath("$.endAt").value("11:00:00"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("룸이 없는 생성 요청은 스케줄을 생성하지 않고 거부한다")
    void rejectsCreateRequestWithoutRoom() throws Exception {
        var body =
                """
                {"dayOfWeek":"MONDAY","startAt":"09:00:00","endAt":"11:00:00"}
                """;

        mockMvc.perform(post(SCHEDULES_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createScheduleUseCase);
    }

    @Test
    @DisplayName("종료가 시작과 같은 생성 요청은 스케줄을 생성하지 않고 거부한다")
    void rejectsCreateRequestWithIntervalEndingAtStart() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","dayOfWeek":"MONDAY",\
                "startAt":"11:00:00","endAt":"11:00:00"}
                """;

        mockMvc.perform(post(SCHEDULES_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("must be after 11:00."));

        verifyNoInteractions(createScheduleUseCase);
    }

    @Test
    @DisplayName("존재하지 않는 룸에 대한 생성은 404 problem detail로 답한다")
    void returnsNotFoundWhenRoomIsMissing() throws Exception {
        when(createScheduleUseCase.create(oneSlotCommand()))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(
                        post(SCHEDULES_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("schedule does not exist."));
    }

    @Test
    @DisplayName("중복 요청은 거부하지 않고 요청된 스케줄들을 201 목록으로 반환한다")
    void createsBatchAndReturnsCreatedSlots() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","schedules":[\
                {"dayOfWeek":"MONDAY","startAt":"09:00:00","endAt":"11:00:00"},\
                {"dayOfWeek":"TUESDAY","startAt":"13:00:00","endAt":"15:00:00"}]}
                """;
        when(createScheduleUseCase.create(batchCommand()))
                .thenReturn(CreateSchedulesResult.from(List.of(slot(), secondSlot())));

        mockMvc.perform(
                        post(SCHEDULES_URL + "/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scheduleId").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$[1].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$[1].startAt").value("13:00:00"));
    }

    @Test
    @DisplayName("빈 목록의 배치 요청은 스케줄을 생성하지 않고 거부한다")
    void rejectsEmptyBatchRequest() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","schedules":[]}
                """;

        mockMvc.perform(
                        post(SCHEDULES_URL + "/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createScheduleUseCase);
    }

    @Test
    @DisplayName("방의 반복 슬롯과 요청 시점의 활성 차단을 함께 200으로 반환한다")
    void listsRoomSlotsWithActiveBlackouts() throws Exception {
        when(listScheduleUseCase.list(listQuery()))
                .thenReturn(
                        ListScheduleResult.of(
                                RoomId.of(ROOM_ID), List.of(DaySchedule.from(slot(), blackout()))));

        mockMvc.perform(
                        get(SCHEDULES_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("now", NOW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.schedules.length()").value(1))
                .andExpect(jsonPath("$.schedules[0].scheduleId").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$.schedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.schedules[0].startAt").value("09:00:00"))
                .andExpect(jsonPath("$.schedules[0].endAt").value("11:00:00"))
                .andExpect(
                        jsonPath("$.schedules[0].blackouts[0].blackoutScheduleId")
                                .value(BLACKOUT_ID.toString()))
                .andExpect(
                        jsonPath("$.schedules[0].blackouts[0].startsAt")
                                .value(CREATED_AT.toString()))
                .andExpect(
                        jsonPath("$.schedules[0].blackouts[0].endsAt")
                                .value(BLACKOUT_END.toString()))
                .andExpect(jsonPath("$.schedules[0].blackouts[0].reason").value("Maintenance"));
    }

    @Test
    @DisplayName("차단이 없는 슬롯은 빈 차단 목록으로 200으로 답한다")
    void listsSlotWithEmptyBlackouts() throws Exception {
        when(listScheduleUseCase.list(listQuery()))
                .thenReturn(
                        ListScheduleResult.of(
                                RoomId.of(ROOM_ID), List.of(DaySchedule.from(slot(), List.of()))));

        mockMvc.perform(
                        get(SCHEDULES_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("now", NOW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[0].blackouts").isArray())
                .andExpect(jsonPath("$.schedules[0].blackouts").isEmpty());
    }

    @Test
    @DisplayName("슬롯이 없는 방은 빈 스케줄 목록으로 200으로 답한다")
    void returnsEmptyScheduleListWhenRoomOffersNoSlots() throws Exception {
        when(listScheduleUseCase.list(listQuery()))
                .thenReturn(ListScheduleResult.of(RoomId.of(ROOM_ID), List.of()));

        mockMvc.perform(
                        get(SCHEDULES_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("now", NOW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.schedules").isArray())
                .andExpect(jsonPath("$.schedules").isEmpty());
    }

    @Test
    @DisplayName("roomId 없는 목록 요청은 조회하지 않고 400으로 거부한다")
    void rejectsListRequestWithoutRoomId() throws Exception {
        mockMvc.perform(get(SCHEDULES_URL).param("now", NOW.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listScheduleUseCase);
    }

    @Test
    @DisplayName("now가 시각이 아닌 목록 요청은 조회하지 않고 400으로 거부한다")
    void rejectsListRequestWithNonInstantNow() throws Exception {
        mockMvc.perform(
                        get(SCHEDULES_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("now", "today"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listScheduleUseCase);
    }

    @Test
    @DisplayName("존재하지 않는 방의 목록 요청은 404 problem detail로 답한다")
    void returnsNotFoundWhenListedRoomIsMissing() throws Exception {
        when(listScheduleUseCase.list(listQuery()))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(
                        get(SCHEDULES_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("now", NOW.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("schedule does not exist."));
    }

    @Test
    @DisplayName("스케줄을 교체하고 새 식별자를 담고 변경되지 않은 시각으로 답한다")
    void replacesSlotAndReturnsReplacementSlot() throws Exception {
        var body =
                """
                {"dayOfWeek":"TUESDAY","startAt":"13:00:00","endAt":"15:00:00"}
                """;
        var command =
                UpdateScheduleCommand.of(
                        ScheduleId.of(SCHEDULE_ID),
                        LocalTimeRange.of(LocalTime.parse("13:00"), LocalTime.parse("15:00")),
                        DayOfWeek.TUESDAY);
        when(updateScheduleUseCase.update(command))
                .thenReturn(UpdateScheduleResult.from(replacementSlot()));

        mockMvc.perform(put(SCHEDULE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(REPLACEMENT_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("폐기된 스케줄 교체는 404 problem detail로 답한다")
    void returnsNotFoundWhenReplaceTargetIsRetired() throws Exception {
        var body =
                """
                {"dayOfWeek":"TUESDAY","startAt":"13:00:00","endAt":"15:00:00"}
                """;
        when(updateScheduleUseCase.update(
                        UpdateScheduleCommand.of(
                                ScheduleId.of(SCHEDULE_ID),
                                LocalTimeRange.of(
                                        LocalTime.parse("13:00"), LocalTime.parse("15:00")),
                                DayOfWeek.TUESDAY)))
                .thenThrow(new NoSuchElementException("schedule does not exists."));

        mockMvc.perform(put(SCHEDULE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("schedule does not exist."));
    }

    @Test
    @DisplayName("룸이 이미 제공하는 슬롯과 겹치는 생성은 409 problem detail로 답한다")
    void returnsConflictWhenSlotOverlapsTheRoomOffer() throws Exception {
        when(createScheduleUseCase.create(oneSlotCommand()))
                .thenThrow(new DataIntegrityViolationException("ex_schedule_no_overlap"));

        mockMvc.perform(
                        post(SCHEDULES_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.detail")
                                .value("schedule overlaps a slot the room already offers."));
    }

    @Test
    @DisplayName("폐기 요청은 대문을 두고도 본문 없이 204로 답한다")
    void retiresSlotWithoutResponseBody() throws Exception {
        mockMvc.perform(delete(SCHEDULE_URL))
                .andExpect(status().isNoContent())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    @DisplayName("식별자 형태가 맞지 않으면 400으로 거부한다")
    void rejectsMalformedScheduleId() throws Exception {
        mockMvc.perform(delete(SCHEDULES_URL + "/not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(deleteScheduleUseCase);
    }

    private String createBody() {
        return """
            {"roomId":"00000000-0000-0000-0000-000000000001","dayOfWeek":"MONDAY",\
            "startAt":"09:00:00","endAt":"11:00:00"}
            """;
    }

    private CreateScheduleCommand oneSlotCommand() {
        return CreateScheduleCommand.of(RoomId.of(ROOM_ID), START_AT, END_AT, DayOfWeek.MONDAY);
    }

    private CreateSchedulesCommand batchCommand() {
        return CreateSchedulesCommand.of(
                RoomId.of(ROOM_ID),
                Map.of(
                        DayOfWeek.MONDAY,
                        List.of(
                                LocalTimeRange.of(
                                        LocalTime.parse("09:00"), LocalTime.parse("11:00"))),
                        DayOfWeek.TUESDAY,
                        List.of(
                                LocalTimeRange.of(
                                        LocalTime.parse("13:00"), LocalTime.parse("15:00")))));
    }

    private ListScheduleQuery listQuery() {
        return ListScheduleQuery.of(RoomId.of(ROOM_ID), NOW);
    }

    private List<BlackoutSchedule> blackout() {
        return List.of(
                BlackoutSchedule.of(
                        BlackoutScheduleId.of(BLACKOUT_ID),
                        RoomId.of(ROOM_ID),
                        ScheduleId.of(SCHEDULE_ID),
                        "Maintenance",
                        CREATED_AT,
                        BLACKOUT_END,
                        CREATED_AT));
    }

    private Schedule slot() {
        return Schedule.of(
                ScheduleId.of(SCHEDULE_ID),
                RoomId.of(ROOM_ID),
                DayOfWeek.MONDAY,
                START_AT,
                END_AT,
                CREATED_AT);
    }

    private Schedule secondSlot() {
        return Schedule.of(
                ScheduleId.of(REPLACEMENT_ID),
                RoomId.of(ROOM_ID),
                DayOfWeek.TUESDAY,
                LocalTime.parse("13:00"),
                LocalTime.parse("15:00"),
                CREATED_AT);
    }

    private Schedule replacementSlot() {
        return Schedule.of(
                ScheduleId.of(REPLACEMENT_ID),
                RoomId.of(ROOM_ID),
                DayOfWeek.TUESDAY,
                LocalTime.parse("13:00"),
                LocalTime.parse("15:00"),
                CREATED_AT);
    }
}
