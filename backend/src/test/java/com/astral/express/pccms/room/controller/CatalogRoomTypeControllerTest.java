package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.room.dto.compatibility.CreateRoomTypeRequest;
import com.astral.express.pccms.room.dto.compatibility.UpdateRoomTypeRequest;
import com.astral.express.pccms.room.dto.compatibility.LegacyRoomTypeResponse;
import com.astral.express.pccms.room.service.RoomAdminService;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogRoomTypeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomAdminService roomAdminService;

    @InjectMocks
    private CatalogRoomTypeController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_success() throws Exception {
        RoomTypeResponse response = new RoomTypeResponse(UUID.randomUUID(), "C1", "N1", 1, 100L, "Desc", true);
        given(roomAdminService.createRoomType(any())).willReturn(response);

        mockMvc.perform(post("/v1/catalog/room-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE\",\"name\":\"Name\",\"defaultCapacity\":1,\"baseDailyPriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void create_null_price_success() throws Exception {
        RoomTypeResponse response = new RoomTypeResponse(UUID.randomUUID(), "C1", "N1", 1, null, "Desc", true);
        given(roomAdminService.createRoomType(any())).willReturn(response);

        controller.create(new CreateRoomTypeRequest("CODE", "Name", 1, null, "Desc", true));
    }

    @Test
    void update_success() throws Exception {
        UUID id = UUID.randomUUID();
        RoomTypeResponse response = new RoomTypeResponse(id, "C1", "N1", 1, 100L, "Desc", true);
        given(roomAdminService.updateRoomType(eq(id), any())).willReturn(response);

        mockMvc.perform(put("/v1/catalog/room-types/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE\",\"name\":\"Name\",\"defaultCapacity\":1,\"baseDailyPriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_null_price_success() throws Exception {
        UUID id = UUID.randomUUID();
        RoomTypeResponse response = new RoomTypeResponse(id, "C1", "N1", 1, null, "Desc", true);
        given(roomAdminService.updateRoomType(eq(id), any())).willReturn(response);

        controller.update(id, new UpdateRoomTypeRequest("CODE", "Name", 1, null, "Desc", true));
    }

    @Test
    void getById_success() throws Exception {
        UUID id = UUID.randomUUID();
        RoomTypeResponse response = new RoomTypeResponse(id, "C1", "N1", 1, 100L, "Desc", true);
        given(roomAdminService.getRoomType(id)).willReturn(response);

        mockMvc.perform(get("/v1/catalog/room-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void list_success() throws Exception {
        given(roomAdminService.listRoomTypes(true)).willReturn(List.of());

        mockMvc.perform(get("/v1/catalog/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/catalog/room-types/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
