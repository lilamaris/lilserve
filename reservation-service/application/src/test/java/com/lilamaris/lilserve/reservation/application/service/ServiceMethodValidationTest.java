package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.model.time.LocalTimeRange;
import com.lilamaris.lilserve.reservation.application.port.in.CreateReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateScheduleUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteReservationUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.DeleteSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.RestoreSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateRoomMetadataUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.UpdateSeatCodeUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSchedulesCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteReservationCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.DeleteSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.RestoreSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateRoomMetadataCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.UpdateSeatCodeCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.out.ReleaseSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ReservationSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ReserveSchedulePort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.SeatSavePort;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import com.lilamaris.lilserve.reservation.domain.id.ScheduleId;
import com.lilamaris.lilserve.reservation.domain.id.SeatId;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@DisplayName("서비스 진입점 메서드 검증")
class ServiceMethodValidationTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @ParameterizedTest(name = "{0}")
    @MethodSource({
        "invalidRequests",
        "invalidReservationRequests",
        "invalidDeleteReservationRequests",
        "invalidScheduleRequests"
    })
    @DisplayName("잘못된 요청은 외부 포트를 호출하기 전에 거부한다")
    void rejectsInvalidRequestBeforeCallingPorts(
            String name, Consumer<AnnotationConfigApplicationContext> invocation) {
        var roomFindPort = mock(RoomFindPort.class);
        var roomSavePort = mock(RoomSavePort.class);
        var seatFindPort = mock(SeatFindPort.class);
        var seatSavePort = mock(SeatSavePort.class);
        var reservationSavePort = mock(ReservationSavePort.class);
        var reservationFindPort = mock(ReservationFindPort.class);
        var releaseSchedulePort = mock(ReleaseSchedulePort.class);
        var reserveSchedulePort = mock(ReserveSchedulePort.class);
        var scheduleSavePort = mock(ScheduleSavePort.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MethodValidationPostProcessor.class);
            context.registerBean(RoomFindPort.class, () -> roomFindPort);
            context.registerBean(RoomSavePort.class, () -> roomSavePort);
            context.registerBean(SeatFindPort.class, () -> seatFindPort);
            context.registerBean(SeatSavePort.class, () -> seatSavePort);
            context.registerBean(ReservationSavePort.class, () -> reservationSavePort);
            context.registerBean(ReservationFindPort.class, () -> reservationFindPort);
            context.registerBean(ReleaseSchedulePort.class, () -> releaseSchedulePort);
            context.registerBean(ReserveSchedulePort.class, () -> reserveSchedulePort);
            context.registerBean(ScheduleSavePort.class, () -> scheduleSavePort);
            context.registerBean(Clock.class, Clock::systemUTC);
            context.registerBean(IdGenerator.class, () -> (IdGenerator<UUID>) () -> ID);
            context.register(
                    CreateRoomService.class,
                    CreateSeatService.class,
                    CreateReservationService.class,
                    DeleteReservationService.class,
                    CreateScheduleService.class,
                    FindRoomService.class,
                    FindSeatService.class,
                    UpdateRoomService.class,
                    UpdateSeatService.class);
            context.refresh();

            assertThatThrownBy(() -> invocation.accept(context))
                    .isInstanceOf(ConstraintViolationException.class);

            verifyNoInteractions(
                    roomFindPort,
                    roomSavePort,
                    seatFindPort,
                    seatSavePort,
                    reservationSavePort,
                    reservationFindPort,
                    releaseSchedulePort,
                    reserveSchedulePort,
                    scheduleSavePort);
        }
    }

    private static Stream<Arguments> invalidReservationRequests() {
        var room = RoomId.of(ID);
        var seat = SeatId.of(ID);
        var slots = Set.of(ScheduleId.of(ID));
        var date = LocalDate.of(2026, 11, 9);
        return Stream.of(
                reservationRequest("nullRequest", null),
                reservationRequest(
                        "nullRoom", new CreateReservationCommand(null, seat, slots, date)),
                reservationRequest(
                        "nullSeat", new CreateReservationCommand(room, null, slots, date)),
                reservationRequest(
                        "emptySlots", new CreateReservationCommand(room, seat, Set.of(), date)),
                reservationRequest(
                        "nullDate", new CreateReservationCommand(room, seat, slots, null)));
    }

    private static Stream<Arguments> invalidDeleteReservationRequests() {
        return Stream.of(
                deleteReservationRequest("nullRequest", null),
                deleteReservationRequest("nullReservation", new DeleteReservationCommand(null)));
    }

    private static Arguments deleteReservationRequest(
            String scenario, DeleteReservationCommand command) {
        return arguments(
                "DeleteReservation." + scenario,
                (Consumer<AnnotationConfigApplicationContext>)
                        context -> context.getBean(DeleteReservationUseCase.class).delete(command));
    }

    private static Stream<Arguments> invalidScheduleRequests() {
        var room = RoomId.of(ID);
        var range = LocalTimeRange.of(LocalTime.of(10, 0), LocalTime.of(12, 0));
        var slots = Map.of(DayOfWeek.MONDAY, List.of(range));
        return Stream.of(
                scheduleRequest("nullRequest", null),
                scheduleRequest(
                        "nullRoom", new CreateScheduleCommand(null, range, DayOfWeek.MONDAY)),
                scheduleRequest(
                        "nullRange", new CreateScheduleCommand(room, null, DayOfWeek.MONDAY)),
                scheduleRequest("nullWeekday", new CreateScheduleCommand(room, range, null)),
                schedulesRequest("nullRequest", null),
                schedulesRequest("nullRoom", new CreateSchedulesCommand(null, slots)),
                schedulesRequest("emptyMap", new CreateSchedulesCommand(room, Map.of())));
    }

    private static Arguments reservationRequest(String scenario, CreateReservationCommand command) {
        return arguments(
                "CreateReservation." + scenario,
                (Consumer<AnnotationConfigApplicationContext>)
                        context -> context.getBean(CreateReservationUseCase.class).create(command));
    }

    private static Arguments scheduleRequest(String scenario, CreateScheduleCommand command) {
        return arguments(
                "CreateSchedule." + scenario,
                (Consumer<AnnotationConfigApplicationContext>)
                        context -> context.getBean(CreateScheduleUseCase.class).create(command));
    }

    private static Arguments schedulesRequest(String scenario, CreateSchedulesCommand command) {
        return arguments(
                "CreateSchedules." + scenario,
                (Consumer<AnnotationConfigApplicationContext>)
                        context -> context.getBean(CreateScheduleUseCase.class).create(command));
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                arguments(
                        "CreateRoom.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(CreateRoomUseCase.class).create(null)),
                arguments(
                        "CreateRoom.invalidField",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(CreateRoomUseCase.class)
                                                .create(
                                                        new CreateRoomCommand(
                                                                "", "name", "description"))),
                arguments(
                        "CreateSeat.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(CreateSeatUseCase.class).create(null)),
                arguments(
                        "CreateSeat.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(CreateSeatUseCase.class)
                                                .create(new CreateSeatCommand(null, "A-01"))),
                arguments(
                        "CreateSeat.invalidField",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(CreateSeatUseCase.class)
                                                .create(new CreateSeatCommand(RoomId.of(ID), ""))),
                arguments(
                        "FindRoom.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(FindRoomUseCase.class).find(null)),
                arguments(
                        "FindRoom.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(FindRoomUseCase.class)
                                                .find(new FindRoomQuery(null))),
                arguments(
                        "FindSeat.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(FindSeatUseCase.class).find(null)),
                arguments(
                        "FindSeat.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(FindSeatUseCase.class)
                                                .find(new FindSeatQuery(null))),
                arguments(
                        "UpdateRoomCode.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomCodeUseCase.class)
                                                .updateCode(null)),
                arguments(
                        "UpdateRoomCode.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomCodeUseCase.class)
                                                .updateCode(
                                                        new UpdateRoomCodeCommand(
                                                                null, "ROOM-01"))),
                arguments(
                        "UpdateRoomCode.invalidField",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomCodeUseCase.class)
                                                .updateCode(
                                                        new UpdateRoomCodeCommand(
                                                                RoomId.of(ID), ""))),
                arguments(
                        "UpdateRoomMetadata.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomMetadataUseCase.class)
                                                .updateMetadata(null)),
                arguments(
                        "UpdateRoomMetadata.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomMetadataUseCase.class)
                                                .updateMetadata(
                                                        new UpdateRoomMetadataCommand(
                                                                null, "name", "description"))),
                arguments(
                        "UpdateRoomMetadata.invalidField",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateRoomMetadataUseCase.class)
                                                .updateMetadata(
                                                        new UpdateRoomMetadataCommand(
                                                                RoomId.of(ID), "new name", null))),
                arguments(
                        "UpdateSeatCode.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateSeatCodeUseCase.class)
                                                .updateCode(null)),
                arguments(
                        "UpdateSeatCode.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateSeatCodeUseCase.class)
                                                .updateCode(
                                                        new UpdateSeatCodeCommand(null, "A-01"))),
                arguments(
                        "UpdateSeatCode.invalidField",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(UpdateSeatCodeUseCase.class)
                                                .updateCode(
                                                        new UpdateSeatCodeCommand(
                                                                SeatId.of(ID), ""))),
                arguments(
                        "DeleteRoom.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(DeleteRoomUseCase.class).delete(null)),
                arguments(
                        "DeleteRoom.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(DeleteRoomUseCase.class)
                                                .delete(new DeleteRoomCommand(null))),
                arguments(
                        "DeleteSeat.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(DeleteSeatUseCase.class).delete(null)),
                arguments(
                        "DeleteSeat.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(DeleteSeatUseCase.class)
                                                .delete(new DeleteSeatCommand(null))),
                arguments(
                        "RestoreRoom.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(RestoreRoomUseCase.class).restore(null)),
                arguments(
                        "RestoreRoom.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(RestoreRoomUseCase.class)
                                                .restore(new RestoreRoomCommand(null))),
                arguments(
                        "RestoreSeat.nullRequest",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context -> context.getBean(RestoreSeatUseCase.class).restore(null)),
                arguments(
                        "RestoreSeat.nullIdentifier",
                        (Consumer<AnnotationConfigApplicationContext>)
                                context ->
                                        context.getBean(RestoreSeatUseCase.class)
                                                .restore(new RestoreSeatCommand(null))));
    }
}
