package com.lilamaris.lilserve.reservation.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateReservationResult;
import com.lilamaris.lilserve.reservation.domain.id.*;
import com.lilamaris.lilserve.reservation.web.controller.ReservationController;
import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("예약 생성 웹 API")
@WebMvcTest(controllers = ReservationController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({
    ReservationController.class,
    ReservationExceptionHandler.class,
    ApiVersionConfiguration.class
})
class ReservationControllerTest {
    private static final String URL = "/api/v1/reservations";
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SECOND_SLOT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESERVATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String BODY =
            """
            {"roomId":"00000000-0000-0000-0000-000000000001",
            "seatId":"00000000-0000-0000-0000-000000000002",
            "scheduleIds":["00000000-0000-0000-0000-000000000003","00000000-0000-0000-0000-000000000004"],
            "reservationDate":"2026-11-16"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CreateReservationUseCase createReservationUseCase;

    @Test
    @DisplayName("예약 생성 요청을 유스케이스에 전달하고 생성 결과 전체를 201로 반환한다")
    void createsReservationAndReturnsCreatedSnapshot() throws Exception {
        var command = command();
        when(createReservationUseCase.create(command))
                .thenReturn(
                        new CreateReservationResult(
                                ReservationId.of(RESERVATION_ID),
                                command.roomId(),
                                command.seatId(),
                                command.scheduleIds(),
                                DATE,
                                "Asia/Seoul",
                                CREATED_AT));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.seatId").value(SEAT_ID.toString()))
                .andExpect(
                        jsonPath("$.scheduleIds")
                                .value(
                                        containsInAnyOrder(
                                                SLOT_ID.toString(), SECOND_SLOT_ID.toString())))
                .andExpect(jsonPath("$.reservationDate").value(DATE.toString()))
                .andExpect(jsonPath("$.timeZone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"roomId", "seatId", "scheduleIds", "reservationDate"})
    @DisplayName("필수 항목이 없으면 예약을 생성하지 않고 400으로 거부한다")
    void rejectsMissingRequiredField(String field) throws Exception {
        var body =
                BODY.replaceAll(
                        "\"" + field + "\":(\"[^\"]*\"|\\[[^\\]]*\\])", "\"" + field + "\":null");
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createReservationUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]"})
    @DisplayName("슬롯 목록이 비거나 null 요소를 포함하면 예약을 생성하지 않는다")
    void rejectsInvalidSlotSelection(String slots) throws Exception {
        var body = BODY.replaceAll("\\[[^\\]]*\\]", slots);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createReservationUseCase);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002",
                "00000000-0000-0000-0000-000000000003",
                "2026-11-16"
            })
    @DisplayName("식별자나 날짜 형식이 잘못되면 예약을 생성하지 않고 400으로 거부한다")
    void rejectsMalformedRequestValue(String value) throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.replace(value, "invalid")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createReservationUseCase);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "room does not exists.",
                "seat does not exists.",
                "schedule does not found."
            })
    @DisplayName("예약에 필요한 자원이 없으면 404 문제 응답을 반환한다")
    void returnsNotFoundForMissingResource(String message) throws Exception {
        when(createReservationUseCase.create(command()))
                .thenThrow(new NoSuchElementException(message));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("room, seat or schedule does not exist."));
    }

    @Test
    @DisplayName("좌석이 요청한 룸에 속하지 않으면 400 문제 응답을 반환한다")
    void rejectsSeatFromAnotherRoom() throws Exception {
        when(createReservationUseCase.create(command()))
                .thenThrow(new IllegalArgumentException("seat must be belong to room."));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("seat must be belong to room."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"room usage is blocked by blackout.", "failed to reserve schedules."})
    @DisplayName("이용 차단 또는 슬롯 확보 실패는 409 문제 응답을 반환한다")
    void returnsConflictForUnavailableReservation(String message) throws Exception {
        when(createReservationUseCase.create(command()))
                .thenThrow(new IllegalStateException(message));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    @DisplayName("저장 무결성 충돌은 데이터베이스 상세 없이 409 문제 응답을 반환한다")
    void returnsConflictWithoutExposingDatabaseDetails() throws Exception {
        when(createReservationUseCase.create(command()))
                .thenThrow(new DataIntegrityViolationException("database constraint details"));
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("reservation conflicts with existing data."));
    }

    @Test
    @DisplayName("지원하지 않는 API 버전으로는 예약을 생성하지 않는다")
    void rejectsUnsupportedVersion() throws Exception {
        mockMvc.perform(
                        post("/api/v2/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createReservationUseCase);
    }

    private CreateReservationCommand command() {
        return CreateReservationCommand.of(
                RoomId.of(ROOM_ID),
                SeatId.of(SEAT_ID),
                Set.of(ScheduleId.of(SLOT_ID), ScheduleId.of(SECOND_SLOT_ID)),
                DATE);
    }
}
