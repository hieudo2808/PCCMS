package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.service.RoomManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoomManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomManagementService roomManagementService;

    @InjectMocks
    private RoomManagementController roomManagementController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomManagementController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnValidationFailed_when_TC_ROOM_010_invalidStatus() throws Exception {
        String request = """
                {
                  "roomCode": "ROOM-BAD-STATUS",
                  "name": "Bad Status",
                  "roomTypeId": "00000000-0000-0000-0000-000000000001",
                  "capacity": 2,
                  "statusCode": "BROKEN"
                }
                """;

        mockMvc.perform(post("/v1/admin/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnOk_when_SearchRoomsWithAvailableStatus() throws Exception {
        RoomResponse room = new RoomResponse(
                UUID.randomUUID(),
                "R001",
                "Room 1",
                UUID.randomUUID(),
                "Standard",
                1,
                2,
                RoomStatus.AVAILABLE,
                ""
        );
        PageRequest pageable = PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("createdAt").descending());
        given(roomManagementService.searchRooms(eq(null), eq(RoomStatus.AVAILABLE), eq(pageable)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of(room), pageable, 1)));

        mockMvc.perform(get("/v1/admin/rooms")
                        .param("statusCode", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data.content[0].statusCode").value("AVAILABLE"));

        verify(roomManagementService).searchRooms(null, RoomStatus.AVAILABLE, pageable);
    }
}
