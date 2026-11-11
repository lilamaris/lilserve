package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.reservation.application.config.ApplicationConfiguration;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateScheduleCommand;
import com.lilamaris.lilserve.reservation.application.port.out.RoomLockFindPort;
import com.lilamaris.lilserve.reservation.application.port.out.ScheduleSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.Schedule;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

@DisplayName("스케줄 생성 유스케이스 구성")
class CreateScheduleServiceConfigurationTest {
    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final RoomId ROOM = RoomId.of(ROOM_ID);

    @ParameterizedTest
    @ValueSource(strings = {"+00:00", "+09:00"})
    @DisplayName("지정된 시간대의 시계와 시간 순 UUID 생성기로 스케줄을 생성한다")
    void createsScheduleWithConfiguredClockAndIdGenerator(String timezone) {
        var scheduleSavePort = mock(ScheduleSavePort.class);
        var roomLockFindPort = mock(RoomLockFindPort.class);
        var room =
                Room.of(
                        ROOM,
                        "ROOM-01",
                        "main hall",
                        "capacity 100",
                        Instant.parse("2026-01-01T00:00:00Z"));
        when(roomLockFindPort.findByIdWithSharedLock(any(RoomId.class)))
                .thenReturn(Optional.of(room));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test", Map.of("lilserve.timezone", timezone)));
            context.registerBean(RoomLockFindPort.class, () -> roomLockFindPort);
            context.registerBean(ScheduleSavePort.class, () -> scheduleSavePort);
            context.register(ApplicationConfiguration.class, CreateScheduleService.class);
            context.refresh();

            var service = context.getBean(CreateScheduleService.class);
            var before = Instant.now();

            var result =
                    service.create(
                            CreateScheduleCommand.of(
                                    ROOM,
                                    LocalTime.of(10, 0),
                                    LocalTime.of(11, 0),
                                    DayOfWeek.MONDAY));

            var after = Instant.now();
            var captor = ArgumentCaptor.forClass(Schedule.class);
            verify(scheduleSavePort).save(captor.capture());
            var schedule = captor.getValue();

            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.of(timezone));
            assertThat(schedule.getCreatedAt()).isBetween(before, after);
            assertThat(schedule.getRoomId()).isEqualTo(ROOM);
            assertThat(schedule.getId().getValue().version()).isEqualTo(7);
            assertThat(result.scheduleId()).isEqualTo(schedule.getId());
            assertThat(result.createdAt()).isEqualTo(schedule.getCreatedAt());
        }
    }
}
