package com.lilamaris.lilserve.reservation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.lilamaris.lilserve.reservation.application.port.in.ListSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutSchedulePeriodUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateBlackoutScheduleReasonUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.web.controller.BlackoutScheduleController;
import com.lilamaris.lilserve.reservation.web.controller.RoomController;
import com.lilamaris.lilserve.reservation.web.controller.ScheduleController;
import com.lilamaris.lilserve.reservation.web.controller.SeatController;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@DisplayName("OpenAPI 작업 식별자")
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
class OpenApiOperationIdTest {
    private static final Map<String, String> EXPECTED_IDS =
            Map.ofEntries(
                    Map.entry("post /api/1/rooms", "createRoom"),
                    Map.entry("get /api/1/rooms", "listRooms"),
                    Map.entry("get /api/1/rooms/{roomId}", "findRoom"),
                    Map.entry("put /api/1/rooms/{roomId}", "updateRoomMetadata"),
                    Map.entry("patch /api/1/rooms/{roomId}/code", "updateRoomCode"),
                    Map.entry("delete /api/1/rooms/{roomId}", "deleteRoom"),
                    Map.entry("post /api/1/rooms/{roomId}/restore", "restoreRoom"),
                    Map.entry("post /api/1/seats", "createSeat"),
                    Map.entry("get /api/1/seats", "listSeats"),
                    Map.entry("get /api/1/seats/{seatId}", "findSeat"),
                    Map.entry("patch /api/1/seats/{seatId}/code", "updateSeatCode"),
                    Map.entry("delete /api/1/seats/{seatId}", "deleteSeat"),
                    Map.entry("post /api/1/seats/{seatId}/restore", "restoreSeat"),
                    Map.entry("post /api/1/schedules", "createSchedule"),
                    Map.entry("post /api/1/schedules/batch", "createSchedules"),
                    Map.entry("put /api/1/schedules/{scheduleId}", "updateSchedule"),
                    Map.entry("delete /api/1/schedules/{scheduleId}", "deleteSchedule"),
                    Map.entry("post /api/1/blackouts", "createBlackoutSchedule"),
                    Map.entry(
                            "patch /api/1/blackouts/{blackoutScheduleId}/period",
                            "updateBlackoutSchedulePeriod"),
                    Map.entry(
                            "patch /api/1/blackouts/{blackoutScheduleId}/reason",
                            "updateBlackoutScheduleReason"),
                    Map.entry(
                            "delete /api/1/blackouts/{blackoutScheduleId}",
                            "deleteBlackoutSchedule"));

    @Autowired private MockMvc mockMvc;

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
}
