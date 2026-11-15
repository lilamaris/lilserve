package com.lilamaris.lilserve.reservation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lilamaris.lilserve.kernel.pagination.OffsetPage;
import com.lilamaris.lilserve.kernel.pagination.OffsetPageRequest;
import com.lilamaris.lilserve.reservation.application.port.in.query.ListRoomQuery;
import com.lilamaris.lilserve.reservation.application.port.in.result.FindRoomResult;
import com.lilamaris.lilserve.reservation.application.port.out.RoomFindPort;
import com.lilamaris.lilserve.reservation.domain.Room;
import com.lilamaris.lilserve.reservation.domain.id.RoomId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("방 목록 조회 유스케이스")
class ListRoomServiceTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final RoomId ROOM_ID = RoomId.of(UUID.randomUUID());
    @Mock private RoomFindPort roomFindPort;
    private ListRoomService service;

    @BeforeEach
    void setUp() {
        service = new ListRoomService(roomFindPort);
    }

    @Test
    @DisplayName("목록 항목을 결과로 변환하고 페이지 번호와 크기 및 전체 건수를 보존한다")
    void mapsItemsAndPreservesPageMetadata() {
        var entity = Room.of(ROOM_ID, "A-01", "Main hall", "Description", CREATED);
        var query = ListRoomQuery.of(" Main ", " A- ", OffsetPageRequest.of(1, 2));
        when(roomFindPort.findNotDeletedByFilters("Main", "A-", query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(entity), 1, 2, 3));

        var result = service.list(query);

        assertThat(result.items()).containsExactly(FindRoomResult.from(entity));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("범위를 벗어난 페이지는 빈 목록과 기존 전체 건수를 반환한다")
    void preservesTotalForEmptyPage() {
        var query = ListRoomQuery.of(null, null, OffsetPageRequest.of(3, 2));
        when(roomFindPort.findNotDeletedByFilters(null, null, query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(), 3, 2, 3));

        var result = service.list(query);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제 목록: 목록 항목을 결과로 변환하고 페이지 번호와 크기 및 전체 건수를 보존한다")
    void mapsDeletedItemsAndPreservesPageMetadata() {
        var entity = Room.of(ROOM_ID, "A-01", "Main hall", "Description", CREATED);
        entity.delete(CREATED.plusSeconds(1));
        var query = ListRoomQuery.of(" Main ", " A- ", OffsetPageRequest.of(1, 2));
        when(roomFindPort.findDeletedByFilters("Main", "A-", query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(entity), 1, 2, 3));

        var result = service.listDeleted(query);

        assertThat(result.items()).containsExactly(FindRoomResult.from(entity));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("삭제 목록: 범위를 벗어난 페이지는 빈 목록과 기존 전체 건수를 반환한다")
    void preservesDeletedTotalForEmptyPage() {
        var query = ListRoomQuery.of(null, null, OffsetPageRequest.of(3, 2));
        when(roomFindPort.findDeletedByFilters(null, null, query.pageRequest()))
                .thenReturn(OffsetPage.of(List.of(), 3, 2, 3));

        var result = service.listDeleted(query);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제된 방 목록 조회는 읽기 전용 트랜잭션을 사용한다")
    void listsDeletedRoomsInReadOnlyTransaction() throws NoSuchMethodException {
        var source = new AnnotationTransactionAttributeSource();
        var method = ListRoomService.class.getMethod("listDeleted", ListRoomQuery.class);

        var attribute = source.getTransactionAttribute(method, ListRoomService.class);

        assertThat(attribute).isNotNull();
        assertThat(attribute.isReadOnly()).isTrue();
    }
}
