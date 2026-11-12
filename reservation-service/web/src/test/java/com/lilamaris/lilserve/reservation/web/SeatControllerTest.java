package com.lilamaris.lilserve.reservation.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateSeatResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindSeatResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateSeatResult;
import com.lilamaris.lilserve.reservation.domain.Seat;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import com.lilamaris.lilserve.reservation.web.controller.SeatController;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("좌석 웹 API")
@WebMvcTest(controllers = SeatController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({SeatController.class, SeatExceptionHandler.class, ApiVersionConfiguration.class})
class SeatControllerTest {
    private static final UUID SEAT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final String CODE = "SEAT-01";
    private static final String SEATS_URL = "/api/v1/seats";
    private static final String SEAT_URL = SEATS_URL + "/" + SEAT_ID;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateSeatUseCase createSeatUseCase;
    @MockitoBean private FindSeatUseCase findSeatUseCase;
    @MockitoBean private ListSeatUseCase listSeatUseCase;
    @MockitoBean private UpdateSeatCodeUseCase updateSeatCodeUseCase;
    @MockitoBean private DeleteSeatUseCase deleteSeatUseCase;
    @MockitoBean private RestoreSeatUseCase restoreSeatUseCase;

    @Test
    @DisplayName("요청 본문으로 좌석을 생성하고 생성된 좌석을 201로 반환한다")
    void createsSeatAndReturnsCreatedSeat() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","code":"SEAT-01"}
                """;
        when(createSeatUseCase.create(CreateSeatCommand.of(RoomId.of(ROOM_ID), CODE)))
                .thenReturn(CreateSeatResult.from(seat()));

        mockMvc.perform(post(SEATS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatId").value(SEAT_ID.toString()))
                .andExpect(jsonPath("$.roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.code").value(CODE))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").isEmpty())
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("빈 코드나 룸 ID가 없는 생성 요청은 좌석을 생성하지 않고 거부한다")
    void rejectsInvalidCreateRequest() throws Exception {
        var body =
                """
                {"roomId":null,"code":""}
                """;

        mockMvc.perform(post(SEATS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createSeatUseCase);
    }

    @Test
    @DisplayName("코드가 상한을 넘는 생성 요청은 좌석을 생성하지 않고 거부한다")
    void rejectsCreateRequestWithCodeAboveLengthLimit() throws Exception {
        var code = "b".repeat(Seat.MAX_CODE_LENGTH + 1);
        var body = "{\"roomId\":\"" + ROOM_ID + "\",\"code\":\"" + code + "\"}";

        mockMvc.perform(post(SEATS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createSeatUseCase);
    }

    @Test
    @DisplayName("룸이 없는 좌석 생성은 404 problem detail로 답한다")
    void returnsNotFoundWhenParentRoomIsMissing() throws Exception {
        var body =
                """
                {"roomId":"00000000-0000-0000-0000-000000000001","code":"SEAT-01"}
                """;
        when(createSeatUseCase.create(CreateSeatCommand.of(RoomId.of(ROOM_ID), CODE)))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(post(SEATS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("seat does not exist."));
    }

    @Test
    @DisplayName("ID로 좌석을 조회하고 마지막 변경까지 반환한다")
    void findsSeatById() throws Exception {
        var seat = seat();
        seat.updateCode("SEAT-02", UPDATED_AT);
        when(findSeatUseCase.find(FindSeatQuery.of(SEAT_ID))).thenReturn(FindSeatResult.from(seat));

        mockMvc.perform(get(SEAT_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SEAT-02"))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("존재하지 않는 좌석 조회는 404 problem detail로 답한다")
    void returnsNotFoundWhenSeatIsMissing() throws Exception {
        when(findSeatUseCase.find(FindSeatQuery.of(SEAT_ID)))
                .thenThrow(new NoSuchElementException("seat does not exists."));

        mockMvc.perform(get(SEAT_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("seat does not exist."));
    }

    @Test
    @DisplayName("식별자 형태가 맞지 않으면 400으로 거부한다")
    void rejectsMalformedSeatId() throws Exception {
        mockMvc.perform(get(SEATS_URL + "/not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(findSeatUseCase);
    }

    @Test
    @DisplayName("좌석의 코드를 변경하고 변경된 좌석을 반환한다")
    void changesSeatCode() throws Exception {
        var body =
                """
                {"code":"SEAT-02"}
                """;
        var seat = seat();
        seat.updateCode("SEAT-02", UPDATED_AT);
        when(updateSeatCodeUseCase.updateCode(
                        UpdateSeatCodeCommand.of(SeatId.of(SEAT_ID), "SEAT-02")))
                .thenReturn(UpdateSeatResult.from(seat));

        mockMvc.perform(
                        patch(SEAT_URL + "/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SEAT-02"))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("코드가 이미 사용된 변경은 409 problem detail로 답한다")
    void returnsConflictWhenCodeIsAlreadyUsed() throws Exception {
        var body =
                """
                {"code":"SEAT-02"}
                """;
        when(updateSeatCodeUseCase.updateCode(
                        UpdateSeatCodeCommand.of(SeatId.of(SEAT_ID), "SEAT-02")))
                .thenThrow(new DataIntegrityViolationException("uk_seat_code"));

        mockMvc.perform(
                        patch(SEAT_URL + "/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("seat code is already used."));
    }

    @Test
    @DisplayName("코드가 상한을 넘는 변경은 좌석을 변경하지 않고 거부한다")
    void rejectsCodeAboveLengthLimit() throws Exception {
        var code = "b".repeat(Seat.MAX_CODE_LENGTH + 1);
        var body = "{\"code\":\"" + code + "\"}";

        mockMvc.perform(
                        patch(SEAT_URL + "/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateSeatCodeUseCase);
    }

    @DisplayName("삭제 요청은 대문을 두고도 본문 없이 204로 답한다")
    @Test
    void deletesSeatWithoutResponseBody() throws Exception {
        var seat = seat();
        seat.delete(UPDATED_AT);
        when(deleteSeatUseCase.delete(DeleteSeatCommand.of(SeatId.of(SEAT_ID))))
                .thenReturn(UpdateSeatResult.from(seat));

        mockMvc.perform(delete(SEAT_URL))
                .andExpect(status().isNoContent())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    @DisplayName("삭제된 좌석을 복원하고 삭제 시각이 비워진 좌석을 반환한다")
    void restoresDeletedSeat() throws Exception {
        var seat = seat();
        seat.delete(UPDATED_AT);
        seat.restore(UPDATED_AT.plusSeconds(60));
        when(restoreSeatUseCase.restore(RestoreSeatCommand.of(SeatId.of(SEAT_ID))))
                .thenReturn(UpdateSeatResult.from(seat));

        mockMvc.perform(post(SEAT_URL + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").isEmpty())
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.plusSeconds(60).toString()));
    }

    @Test
    @DisplayName("삭제되지 않은 좌석 복원은 404 problem detail로 답한다")
    void returnsNotFoundWhenRestoreTargetIsNotDeleted() throws Exception {
        when(restoreSeatUseCase.restore(RestoreSeatCommand.of(SeatId.of(SEAT_ID))))
                .thenThrow(new NoSuchElementException("seat does not exists."));

        mockMvc.perform(post(SEAT_URL + "/restore"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("seat does not exist."));
    }

    @Test
    @DisplayName("목록 조회 조건을 전달하고 식별자가 평탄화된 항목 및 페이지 메타데이터를 반환한다")
    void listsItemsWithPageMetadata() throws Exception {
        var query = ListSeatQuery.of(RoomId.of(ROOM_ID), OffsetPageRequest.of(1, 2));
        when(listSeatUseCase.list(query))
                .thenReturn(OffsetPage.of(List.of(FindSeatResult.from(seat())), 1, 2, 3));

        mockMvc.perform(
                        get(SEATS_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("page", "1")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].seatId").value(SEAT_ID.toString()))
                .andExpect(jsonPath("$.items[0].roomId").value(ROOM_ID.toString()))
                .andExpect(jsonPath("$.items[0].code").value(CODE))
                .andExpect(jsonPath("$.items[0].createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("페이징 인자를 생략하면 첫 페이지와 기본 크기로 조회하고 빈 결과도 200으로 반환한다")
    void listsDefaultEmptyPage() throws Exception {
        when(listSeatUseCase.list(
                        ListSeatQuery.of(RoomId.of(ROOM_ID), OffsetPageRequest.of(0, 20))))
                .thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        mockMvc.perform(get(SEATS_URL).param("roomId", ROOM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "2147483647,2", "invalid,20", "0,invalid", "2147483648,20"})
    @DisplayName("잘못된 페이징 요청은 유스케이스를 실행하지 않고 400으로 거부한다")
    void rejectsInvalidListPagination(String page, String size) throws Exception {
        mockMvc.perform(
                        get(SEATS_URL)
                                .param("roomId", ROOM_ID.toString())
                                .param("page", page)
                                .param("size", size))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listSeatUseCase);
    }

    @Test
    @DisplayName("방 식별자를 생략한 좌석 목록 요청은 400으로 거부한다")
    void rejectsListWithoutRoomId() throws Exception {
        mockMvc.perform(get(SEATS_URL)).andExpect(status().isBadRequest());

        verifyNoInteractions(listSeatUseCase);
    }

    @Test
    @DisplayName("방 식별자가 UUID 형식이 아니면 좌석 목록 요청을 400으로 거부한다")
    void rejectsListWithMalformedRoomId() throws Exception {
        mockMvc.perform(get(SEATS_URL).param("roomId", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listSeatUseCase);
    }

    @Test
    @DisplayName("조회 대상 방이 없거나 삭제되었으면 좌석 목록 조회는 404로 응답한다")
    void rejectsListForUnavailableRoom() throws Exception {
        when(listSeatUseCase.list(
                        ListSeatQuery.of(RoomId.of(ROOM_ID), OffsetPageRequest.of(0, 20))))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(get(SEATS_URL).param("roomId", ROOM_ID.toString()))
                .andExpect(status().isNotFound());
    }

    private Seat seat() {
        return Seat.of(SeatId.of(SEAT_ID), RoomId.of(ROOM_ID), CODE, CREATED_AT);
    }
}
