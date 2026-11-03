package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lilamaris.lilserve.reservation.application.config.ApplicationConfiguration;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.out.RoomSavePort;
import com.lilamaris.lilserve.reservation.domain.Room;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

@DisplayName("룸 생성 유스케이스 구성")
class CreateRoomServiceConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = {"+00:00", "+09:00"})
    @DisplayName("지정된 시간대의 시계와 UUID 생성기로 룸을 생성한다")
    void createsRoomWithConfiguredClockAndIdGenerator(String timezone) {
        var roomSavePort = mock(RoomSavePort.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("test", Map.of("lilserve.timezone", timezone)));
            context.registerBean(RoomSavePort.class, () -> roomSavePort);
            context.register(ApplicationConfiguration.class, CreateRoomService.class);
            context.refresh();
            var service = context.getBean(CreateRoomService.class);
            var before = Instant.now();

            var result =
                    service.create(CreateRoomCommand.of("ROOM-01", "main hall", "capacity 100"));

            var after = Instant.now();
            var captor = ArgumentCaptor.forClass(Room.class);
            verify(roomSavePort).save(captor.capture());
            var room = captor.getValue();
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.of(timezone));
            assertThat(room.getCreatedAt()).isBetween(before, after);
            assertThat(result.createdAt()).isEqualTo(room.getCreatedAt());
            assertThat(result.roomId()).isEqualTo(room.getId());
            assertThat(result.roomId().getValue().version()).isEqualTo(7);
        }
    }
}
