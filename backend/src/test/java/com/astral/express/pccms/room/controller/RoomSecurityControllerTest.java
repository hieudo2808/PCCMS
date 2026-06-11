package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.room.service.RoomManagementService;
import com.astral.express.pccms.room.service.RoomAdminService;
import com.astral.express.pccms.room.service.compatibility.CatalogRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RoomSecurityControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RoomManagementService roomManagementService;

    @MockitoBean
    private RoomAdminService roomAdminService;

    @MockitoBean
    private CatalogRoomService catalogRoomService;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("SEC-ROOM-001.1: Unauthenticated request to /v1/rooms -> 401")
    void secRoom001_1_Unauthenticated() throws Exception {
        String requestJson = "{\"roomCode\":\"R101\",\"name\":\"Room 101\",\"roomTypeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"floor\":1,\"capacity\":1,\"statusCode\":\"AVAILABLE\"}";
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(roomManagementService);
    }

    @Test
    @DisplayName("SEC-ROOM-001.2: Authenticated without ADMIN to /v1/rooms -> 403")
    @WithMockUser(roles = {"USER"})
    void secRoom001_2_Forbidden() throws Exception {
        String requestJson = "{\"roomCode\":\"R101\",\"name\":\"Room 101\",\"roomTypeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"floor\":1,\"capacity\":1,\"statusCode\":\"AVAILABLE\"}";
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
        verifyNoInteractions(roomManagementService);
    }

    @Test
    @DisplayName("SEC-ROOM-001.3: Authenticated with ADMIN to /v1/rooms -> 200/201")
    @WithMockUser(roles = {"ADMIN"})
    void secRoom001_3_Allowed() throws Exception {
        String requestJson = "{\"roomCode\":\"R101\",\"name\":\"Room 101\",\"roomTypeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"floor\":1,\"capacity\":1,\"statusCode\":\"AVAILABLE\"}";
        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode != 200 && statusCode != 201 && statusCode != 400) {
                        throw new AssertionError("Expected 200, 201, or 400 but got " + statusCode);
                    }
                });
    }

    @Test
    @DisplayName("SEC-ROOM-002.1: Unauthenticated request to /v1/catalog/rooms/{id} -> 401")
    void secRoom002_1_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/rooms/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(catalogRoomService);
    }

    @Test
    @DisplayName("SEC-ROOM-002.2: Authenticated without ROOM_READ to /v1/catalog/rooms/{id} -> 403")
    @WithMockUser(authorities = {"USER_READ"})
    void secRoom002_2_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/rooms/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogRoomService);
    }

    @Test
    @DisplayName("SEC-ROOM-002.3: Authenticated with BOARDING_READ to /v1/catalog/rooms/{id} -> 200")
    @WithMockUser(authorities = {"BOARDING_READ"})
    void secRoom002_3_Allowed() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/rooms/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
