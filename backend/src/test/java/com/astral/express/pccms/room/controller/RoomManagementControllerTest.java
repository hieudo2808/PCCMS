package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.service.RoomManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoomManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomManagementService roomManagementService;

    @InjectMocks
    private RoomManagementController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchRooms_success() throws Exception {
        PageResponse<RoomResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(roomManagementService.searchRooms(any(), any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/admin/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRoom_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/v1/admin/rooms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRoom_success() throws Exception {
        mockMvc.perform(post("/v1/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateRoom_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/admin/rooms/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateRoomStatus_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/admin/rooms/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivateRoom_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/admin/rooms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
