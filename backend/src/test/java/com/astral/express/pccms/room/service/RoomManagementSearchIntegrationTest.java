package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.room.entity.RoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThatCode;


class RoomManagementSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RoomManagementService roomManagementService;

    @Test
    void should_SearchRoomsWithCreatedAtSort_without_JpaPathError() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        assertThatCode(() -> roomManagementService.searchRooms(null, RoomStatus.AVAILABLE, pageable))
                .doesNotThrowAnyException();
    }
}
