package com.lilamaris.lilserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilserve.reservation.application.config.ApplicationProperties;
import com.lilamaris.lilserve.reservation.application.model.id.IdGenerator;
import com.lilamaris.lilserve.reservation.application.port.in.CreateRoomUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.CreateSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.FindSeatUseCase;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateRoomCommand;
import com.lilamaris.lilserve.reservation.application.port.in.command.CreateSeatCommand;
import com.lilamaris.lilserve.reservation.application.port.in.query.FindSeatQuery;
import com.lilamaris.lilserve.reservation.application.port.out.SeatFindPort;
import com.lilamaris.lilserve.reservation.persistence.testfixture.PostgresTestConfiguration;
import com.lilamaris.lilserve.reservation.web.controller.SeatController;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Verifies module wiring and room/seat persistence using the real persistence adapters, including
 * resource lock port implementations.
 */
@DisplayName("예약 서비스 실행 모듈")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(PostgresTestConfiguration.class)
class LauncherApplicationTest {
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Autowired private ApplicationContext context;
    @Autowired private CreateRoomUseCase createRoomUseCase;
    @Autowired private CreateSeatUseCase createSeatUseCase;
    @Autowired private FindSeatUseCase findSeatUseCase;

    @Test
    @DisplayName("웹, 애플리케이션, 영속성 빈이 하나의 컨텍스트에 연결된다")
    void wiresWebApplicationAndPersistenceBeans() {
        assertThat(context.getBean(SeatController.class)).isNotNull();
        assertThat(context.getBean(SeatFindPort.class)).isNotNull();
        assertThat(context.getBean(IdGenerator.class)).isNotNull();

        assertThat(context.getBean(ApplicationProperties.class).timezone()).isEqualTo(UTC);
        assertThat(context.getBean(Clock.class).getZone()).isEqualTo(UTC);
    }

    @Test
    @DisplayName("룸 아래 좌석을 만들어 영속성에서 다시 읽는다")
    void createsSeatInRoomAndReadsItBack() {
        var room =
                createRoomUseCase.create(
                        CreateRoomCommand.of("ROOM-01", "main hall", "capacity 100"));

        var created = createSeatUseCase.create(CreateSeatCommand.of(room.roomId(), "SEAT-01"));

        var found = findSeatUseCase.find(FindSeatQuery.of(created.seatId().getValue()));

        assertThat(found.seatId()).isEqualTo(created.seatId());
        assertThat(found.roomId()).isEqualTo(room.roomId());
        assertThat(found.code()).isEqualTo("SEAT-01");
    }
}
