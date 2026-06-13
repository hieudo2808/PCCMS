package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.service.RoomAdminService;
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
class RoomAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomAdminService roomAdminService;

    @InjectMocks
    private RoomAdminController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listActiveRoomTypes_success() throws Exception {
        given(roomAdminService.listActiveRoomTypes()).willReturn(List.of());

        mockMvc.perform(get("/v1/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRoomType_success() throws Exception {
        mockMvc.perform(post("/v1/room-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE\",\"name\":\"Name\",\"defaultCapacity\":1,\"baseDailyPriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void updateRoomType_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/room-types/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE\",\"name\":\"Name\",\"defaultCapacity\":1,\"baseDailyPriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivateRoomType_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/room-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listRooms_success() throws Exception {
        PageResponse<RoomResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(roomAdminService.listRooms(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRoom_success() throws Exception {
        mockMvc.perform(post("/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void updateRoom_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/rooms/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateRoomStatus_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/rooms/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
