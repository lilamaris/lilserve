package com.lilamaris.lilserve.reservation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.CreateBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteBlackoutScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.ListSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutSchedulePeriodUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutScheduleReasonUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.web.controller.BlackoutScheduleController;
import com.lilamaris.lilserve.reservation.web.controller.RoomController;
import com.lilamaris.lilserve.reservation.web.controller.ScheduleController;
import com.lilamaris.lilserve.reservation.web.controller.SeatController;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@DisplayName("OpenAPI 문서와 웹 API 계약")
@WebMvcTest(
        controllers = {
            RoomController.class,
            SeatController.class,
            ScheduleController.class,
            BlackoutScheduleController.class
        })
@ContextConfiguration(classes = WebMvcTestConfiguration.class)
@ImportAutoConfiguration({
    SpringDocConfiguration.class,
    SpringDocConfigProperties.class,
    SpringDocWebMvcConfiguration.class
})
@Import({
    RoomController.class,
    SeatController.class,
    ScheduleController.class,
    BlackoutScheduleController.class,
    ApiVersionConfiguration.class
})
@MockitoBean(
        types = {
            CreateBlackoutScheduleUseCase.class,
            CreateRoomUseCase.class,
            CreateScheduleUseCase.class,
            CreateSeatUseCase.class,
            DeleteBlackoutScheduleUseCase.class,
            DeleteRoomUseCase.class,
            DeleteScheduleUseCase.class,
            DeleteSeatUseCase.class,
            FindRoomUseCase.class,
            FindSeatUseCase.class,
            ListRoomUseCase.class,
            ListScheduleUseCase.class,
            ListSeatUseCase.class,
            RestoreRoomUseCase.class,
            RestoreSeatUseCase.class,
            UpdateBlackoutSchedulePeriodUseCase.class,
            UpdateBlackoutScheduleReasonUseCase.class,
            UpdateRoomCodeUseCase.class,
            UpdateRoomMetadataUseCase.class,
            UpdateScheduleUseCase.class,
            UpdateSeatCodeUseCase.class
        })
class OpenApiContractTest {
    private static final Map<String, String> EXPECTED_IDS =
            Map.ofEntries(
                    Map.entry("post /api/v1/rooms", "createRoom"),
                    Map.entry("get /api/v1/rooms", "listRooms"),
                    Map.entry("get /api/v1/rooms/{roomId}", "findRoom"),
                    Map.entry("put /api/v1/rooms/{roomId}", "updateRoomMetadata"),
                    Map.entry("patch /api/v1/rooms/{roomId}/code", "updateRoomCode"),
                    Map.entry("delete /api/v1/rooms/{roomId}", "deleteRoom"),
                    Map.entry("post /api/v1/rooms/{roomId}/restore", "restoreRoom"),
                    Map.entry("post /api/v1/seats", "createSeat"),
                    Map.entry("get /api/v1/seats", "listSeats"),
                    Map.entry("get /api/v1/seats/{seatId}", "findSeat"),
                    Map.entry("patch /api/v1/seats/{seatId}/code", "updateSeatCode"),
                    Map.entry("delete /api/v1/seats/{seatId}", "deleteSeat"),
                    Map.entry("post /api/v1/seats/{seatId}/restore", "restoreSeat"),
                    Map.entry("post /api/v1/schedules", "createSchedule"),
                    Map.entry("get /api/v1/schedules", "listSchedules"),
                    Map.entry("post /api/v1/schedules/batch", "createSchedules"),
                    Map.entry("put /api/v1/schedules/{scheduleId}", "updateSchedule"),
                    Map.entry("delete /api/v1/schedules/{scheduleId}", "deleteSchedule"),
                    Map.entry("post /api/v1/blackouts", "createBlackoutSchedule"),
                    Map.entry(
                            "patch /api/v1/blackouts/{blackoutScheduleId}/period",
                            "updateBlackoutSchedulePeriod"),
                    Map.entry(
                            "patch /api/v1/blackouts/{blackoutScheduleId}/reason",
                            "updateBlackoutScheduleReason"),
                    Map.entry(
                            "delete /api/v1/blackouts/{blackoutScheduleId}",
                            "deleteBlackoutSchedule"));

    private static final Map<String, List<String>> EXPECTED_REQUIRED =
            Map.ofEntries(
                    Map.entry(
                            "RoomResponse",
                            List.of("roomId", "code", "name", "description", "createdAt")),
                    Map.entry("SeatResponse", List.of("seatId", "roomId", "code", "createdAt")),
                    Map.entry(
                            "ScheduleResponse",
                            List.of(
                                    "scheduleId",
                                    "roomId",
                                    "dayOfWeek",
                                    "startAt",
                                    "endAt",
                                    "createdAt")),
                    Map.entry("ScheduleListResponse", List.of("roomId", "schedules")),
                    Map.entry(
                            "DayScheduleResponse",
                            List.of("scheduleId", "dayOfWeek", "startAt", "endAt", "blackouts")),
                    Map.entry(
                            "ScheduleBlackoutResponse",
                            List.of("blackoutScheduleId", "startsAt", "endsAt", "reason")),
                    Map.entry(
                            "BlackoutScheduleResponse",
                            List.of(
                                    "blackoutScheduleId",
                                    "roomId",
                                    "scheduleId",
                                    "reason",
                                    "startsAt",
                                    "endsAt",
                                    "createdAt")),
                    Map.entry(
                            "RoomPageResponse", List.of("items", "page", "size", "totalElements")),
                    Map.entry(
                            "SeatPageResponse", List.of("items", "page", "size", "totalElements")));

    @Autowired private MockMvc mockMvc;
    @Autowired private ListRoomUseCase listRoomUseCase;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("생성된 문서의 모든 경로와 HTTP 작업은 고정된 고유 식별자를 갖는다")
    void publishesStableUniqueOperationIds() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());

        for (var entry : EXPECTED_IDS.entrySet()) {
            var endpoint = entry.getKey().split(" ", 2);
            result.andExpect(
                    jsonPath("$.paths['" + endpoint[1] + "']." + endpoint[0] + ".operationId")
                            .value(entry.getValue()));
        }
        result.andExpect(
                jsonPath("$.paths..operationId")
                        .value(containsInAnyOrder(EXPECTED_IDS.values().toArray())));
    }

    @Test
    @DisplayName("문서가 제공한 방 목록 URL로 실제 요청하면 성공한다")
    void servesRequestAtDocumentedPath() throws Exception {
        var document =
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Map<String, Map<String, Object>> paths = JsonPath.read(document, "$.paths");
        var path =
                paths.entrySet().stream()
                        .filter(
                                entry ->
                                        entry.getValue().get("get") instanceof Map<?, ?> operation
                                                && "listRooms".equals(operation.get("operationId")))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElseThrow();
        when(listRoomUseCase.list(ListRoomQuery.of(null, null, OffsetPageRequest.of(0, 20))))
                .thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"/api/v2/rooms,400", "/api/v0/rooms,400", "/api/1/rooms,404", "/api/abc/rooms,404"})
    @DisplayName("지원하지 않는 버전과 버전 접두사가 잘못된 경로는 거부한다")
    void rejectsUnsupportedVersionPaths(String path, int statusCode) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().is(statusCode));
    }

    @Test
    @DisplayName("모든 컨트롤러 작업은 메서드명 파생 없이 고유한 식별자를 명시한다")
    void declaresUniqueOperationIdsOnEveryEndpoint() {
        var operations =
                handlerMapping.getHandlerMethods().values().stream()
                        .filter(
                                handler ->
                                        handler.getBeanType()
                                                .getPackageName()
                                                .equals(RoomController.class.getPackageName()))
                        .map(handler -> handler.getMethodAnnotation(Operation.class))
                        .toList();

        assertThat(operations).isNotEmpty().doesNotContainNull();
        assertThat(operations)
                .allSatisfy(operation -> assertThat(operation.operationId()).isNotBlank());
        assertThat(operations).extracting(Operation::operationId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("본문을 게시하는 모든 성공 응답은 application/json 과 예시를 함께 게시한다")
    @SuppressWarnings("unchecked")
    void publishesSuccessBodyAsJsonWithExample() throws Exception {
        Map<String, Map<String, Map<String, Object>>> paths =
                JsonPath.read(openApiDocument(), "$.paths");
        var problems = successBodyProblems(paths);

        assertThat(problems).isEmpty();
    }

    @Test
    @DisplayName("응답 스키마는 항상 존재하는 필드를 required 로 게시한다")
    @SuppressWarnings("unchecked")
    void publishesRequiredFieldsOnResponseSchemas() throws Exception {
        Map<String, Map<String, Object>> schemas =
                JsonPath.read(openApiDocument(), "$.components.schemas");

        for (var entry : EXPECTED_REQUIRED.entrySet()) {
            var schema = schemas.get(entry.getKey());
            assertThat(schema).as("schema %s", entry.getKey()).isNotNull();
            List<String> required = (List<String>) schema.get("required");
            assertThat(required)
                    .as("required of %s", entry.getKey())
                    .containsExactlyInAnyOrderElementsOf(entry.getValue());
        }
    }

    private String openApiDocument() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     * Names every success body that is not published as a single {@code application/json} media
     * type carrying an example.
     */
    @SuppressWarnings("unchecked")
    private List<String> successBodyProblems(Map<String, Map<String, Map<String, Object>>> paths) {
        var problems = new ArrayList<String>();
        paths.forEach(
                (path, operations) ->
                        operations.forEach(
                                (method, operation) ->
                                        responsesOf(operation)
                                                .forEach(
                                                        (code, response) ->
                                                                addProblemWhen(
                                                                        problems,
                                                                        method + " " + path + " "
                                                                                + code,
                                                                        response))));
        return problems;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responsesOf(Map<String, Object> operation) {
        var responses = (Map<String, Object>) operation.get("responses");
        return responses == null ? Map.of() : responses;
    }

    @SuppressWarnings("unchecked")
    private void addProblemWhen(List<String> problems, String endpoint, Object response) {
        if (!endpoint.endsWith("200") && !endpoint.endsWith("201")) return;
        var content = (Map<String, Object>) ((Map<String, Object>) response).get("content");
        if (content == null) return;
        var body = content.get("application/json");
        if (content.size() != 1 || body == null) {
            problems.add(endpoint + " media types " + content.keySet());
            return;
        }
        if (((Map<String, Object>) body).get("example") == null)
            problems.add(endpoint + " has no example");
    }
}
