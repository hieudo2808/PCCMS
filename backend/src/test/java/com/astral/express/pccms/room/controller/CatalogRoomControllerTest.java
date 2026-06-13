package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.room.dto.compatibility.CreateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.UpdateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.LegacyRoomResponse;
import com.astral.express.pccms.room.service.compatibility.CatalogRoomService;
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
class CatalogRoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogRoomService catalogRoomService;

    @InjectMocks
    private CatalogRoomController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_success() throws Exception {
        mockMvc.perform(post("/v1/catalog/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void update_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/catalog/rooms/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomCode\":\"CODE\",\"name\":\"Room\",\"capacity\":1,\"floor\":1,\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"statusCode\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/v1/catalog/rooms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void list_success() throws Exception {
        PageResponse<LegacyRoomResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(catalogRoomService.list(any(), any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/catalog/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/catalog/rooms/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
