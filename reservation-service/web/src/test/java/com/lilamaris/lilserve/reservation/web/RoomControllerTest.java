package com.lilamaris.lilserve.reservation.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.CreateRoomResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.in.result.UpdateRoomResult;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.web.controller.RoomController;
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

@DisplayName("룸 웹 API")
@WebMvcTest(controllers = RoomController.class)
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@Import({RoomController.class, RoomExceptionHandler.class, ApiVersionConfiguration.class})
class RoomControllerTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final String CODE = "ROOM-01";
    private static final String NAME = "main hall";
    private static final String DESCRIPTION = "capacity 100";
    private static final String ROOMS_URL = "/api/v1/rooms";
    private static final String ROOM_URL = ROOMS_URL + "/" + ID;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateRoomUseCase createRoomUseCase;
    @MockitoBean private FindRoomUseCase findRoomUseCase;
    @MockitoBean private ListRoomUseCase listRoomUseCase;
    @MockitoBean private UpdateRoomMetadataUseCase updateRoomMetadataUseCase;
    @MockitoBean private UpdateRoomCodeUseCase updateRoomCodeUseCase;
    @MockitoBean private DeleteRoomUseCase deleteRoomUseCase;
    @MockitoBean private RestoreRoomUseCase restoreRoomUseCase;

    @Test
    @DisplayName("요청 본문으로 룸을 생성하고 생성된 룸을 201로 반환한다")
    void createsRoomAndReturnsCreatedRoom() throws Exception {
        var body =
                """
                {"code":"ROOM-01","name":"main hall","description":"capacity 100"}
                """;
        when(createRoomUseCase.create(CreateRoomCommand.of(CODE, NAME, DESCRIPTION)))
                .thenReturn(CreateRoomResult.from(room()));

        mockMvc.perform(post(ROOMS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(ID.toString()))
                .andExpect(jsonPath("$.code").value(CODE))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").isEmpty())
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("빈 코드나 설명이 상한을 넘는 생성 요청은 룸을 생성하지 않고 거부한다")
    void rejectsInvalidCreateRequest() throws Exception {
        var body =
                """
                {"code":"","name":"main hall","description":"capacity 100"}
                """;

        mockMvc.perform(post(ROOMS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createRoomUseCase);
    }

    @Test
    @DisplayName("ID로 룸을 조회하고 마지막 변경까지 반환한다")
    void findsRoomById() throws Exception {
        var room = room();
        room.updateName("small hall", UPDATED_AT);
        when(findRoomUseCase.find(FindRoomQuery.of(ID))).thenReturn(FindRoomResult.from(room));

        mockMvc.perform(get(ROOM_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("small hall"))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("존재하지 않는 룸 조회는 404 problem detail로 답한다")
    void returnsNotFoundWhenRoomIsMissing() throws Exception {
        when(findRoomUseCase.find(FindRoomQuery.of(ID)))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(get(ROOM_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("room does not exist."));
    }

    @Test
    @DisplayName("식별자 형태가 맞지 않으면 400으로 거부한다")
    void rejectsMalformedRoomId() throws Exception {
        mockMvc.perform(get(ROOMS_URL + "/not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(findRoomUseCase);
    }

    @Test
    @DisplayName("룸의 이름과 설명을 변경하고 변경된 룸을 반환한다")
    void updatesRoomMetadataAndReturnsUpdatedRoom() throws Exception {
        var body =
                """
                {"name":"small hall","description":"capacity 40"}
                """;
        var command = UpdateRoomMetadataCommand.of(RoomId.of(ID), "small hall", "capacity 40");
        var room = room();
        room.updateMetadata("small hall", "capacity 40", UPDATED_AT);
        when(updateRoomMetadataUseCase.updateMetadata(command))
                .thenReturn(UpdateRoomResult.from(room));

        mockMvc.perform(put(ROOM_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("small hall"))
                .andExpect(jsonPath("$.description").value("capacity 40"))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("이름이 상한을 넘는 변경 요청은 룸을 변경하지 않고 거부한다")
    void rejectsNameAboveLengthLimit() throws Exception {
        var name = "b".repeat(Room.MAX_NAME_LENGTH + 1);
        var body = "{\"name\":\"" + name + "\",\"description\":\"capacity 40\"}";

        mockMvc.perform(put(ROOM_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateRoomMetadataUseCase);
    }

    @Test
    @DisplayName("룸의 코드를 변경하고 변경된 룸을 반환한다")
    void changesRoomCode() throws Exception {
        var body =
                """
            {"code":"ROOM-02"}
            """;
        var room = room();
        room.updateCode("ROOM-02", UPDATED_AT);
        when(updateRoomCodeUseCase.updateCode(UpdateRoomCodeCommand.of(RoomId.of(ID), "ROOM-02")))
                .thenReturn(UpdateRoomResult.from(room));

        mockMvc.perform(
                        patch(ROOM_URL + "/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ROOM-02"))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("코드가 이미 사용된 변경은 409 problem detail로 답한다")
    void returnsConflictWhenCodeIsAlreadyUsed() throws Exception {
        var body =
                """
            {"code":"ROOM-02"}
            """;
        when(updateRoomCodeUseCase.updateCode(UpdateRoomCodeCommand.of(RoomId.of(ID), "ROOM-02")))
                .thenThrow(new DataIntegrityViolationException("uk_room_code"));

        mockMvc.perform(
                        patch(ROOM_URL + "/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("room code is already used."));
    }

    @DisplayName("삭제 요청은 대문을 두고도 본문 없이 204로 답한다")
    @Test
    void deletesRoomWithoutResponseBody() throws Exception {
        var room = room();
        room.delete(UPDATED_AT);
        when(deleteRoomUseCase.delete(DeleteRoomCommand.of(RoomId.of(ID))))
                .thenReturn(UpdateRoomResult.from(room));

        mockMvc.perform(delete(ROOM_URL))
                .andExpect(status().isNoContent())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    @DisplayName("삭제된 룸을 복원하고 삭제 시각이 비워진 룸을 반환한다")
    void restoresDeletedRoom() throws Exception {
        var room = room();
        room.delete(UPDATED_AT);
        room.restore(UPDATED_AT.plusSeconds(60));
        when(restoreRoomUseCase.restore(RestoreRoomCommand.of(RoomId.of(ID))))
                .thenReturn(UpdateRoomResult.from(room));

        mockMvc.perform(post(ROOM_URL + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").isEmpty())
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.plusSeconds(60).toString()));
    }

    @Test
    @DisplayName("삭제되지 않은 룸 복원은 404 problem detail로 답한다")
    void returnsNotFoundWhenRestoreTargetIsNotDeleted() throws Exception {
        when(restoreRoomUseCase.restore(RestoreRoomCommand.of(RoomId.of(ID))))
                .thenThrow(new NoSuchElementException("room does not exists."));

        mockMvc.perform(post(ROOM_URL + "/restore"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("room does not exist."));
    }

    @Test
    @DisplayName("목록 조회 조건을 전달하고 식별자가 평탄화된 항목 및 페이지 메타데이터를 반환한다")
    void listsItemsWithPageMetadata() throws Exception {
        var query = ListRoomQuery.of("main", "ROOM", OffsetPageRequest.of(1, 2));
        when(listRoomUseCase.list(query))
                .thenReturn(OffsetPage.of(List.of(FindRoomResult.from(room())), 1, 2, 3));

        mockMvc.perform(
                        get(ROOMS_URL)
                                .param("name", "main")
                                .param("code", "ROOM")
                                .param("page", "1")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].roomId").value(ID.toString()))
                .andExpect(jsonPath("$.items[0].name").value(NAME))
                .andExpect(jsonPath("$.items[0].description").value(DESCRIPTION))
                .andExpect(jsonPath("$.items[0].code").value(CODE))
                .andExpect(jsonPath("$.items[0].createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("페이징 인자를 생략하면 첫 페이지와 기본 크기로 조회하고 빈 결과도 200으로 반환한다")
    void listsDefaultEmptyPage() throws Exception {
        when(listRoomUseCase.list(ListRoomQuery.of(null, null, OffsetPageRequest.of(0, 20))))
                .thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        mockMvc.perform(get(ROOMS_URL))
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
        mockMvc.perform(get(ROOMS_URL).param("page", page).param("size", size))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listRoomUseCase);
    }

    @ParameterizedTest
    @CsvSource({"main,", ",ROOM", "'  ','  '"})
    @DisplayName("단일 필터 또는 빈 필터를 선택적으로 전달할 수 있다")
    void acceptsOptionalListFilters(String name, String code) throws Exception {
        var request = get(ROOMS_URL);
        if (name != null) request.param("name", name);
        if (code != null) request.param("code", code);
        when(listRoomUseCase.list(ListRoomQuery.of(name, code, OffsetPageRequest.of(0, 20))))
                .thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private Room room() {
        return Room.of(RoomId.of(ID), CODE, NAME, DESCRIPTION, CREATED_AT);
    }
}
