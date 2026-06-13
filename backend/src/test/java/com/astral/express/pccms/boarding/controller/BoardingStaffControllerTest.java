package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.service.BoardingStaffService;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.security.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingStaffControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingStaffService boardingStaffService;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private BoardingStaffController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listActiveStays_success() throws Exception {
        mockMvc.perform(get("/v1/boarding/staff/stays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listSessionLogs_success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        mockMvc.perform(get("/v1/boarding/staff/care-logs")
                .param("sessionId", sessionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void upsertCareLog_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        
        mockMvc.perform(post("/v1/boarding/staff/care-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"" + UUID.randomUUID() + "\",\"logDate\":\"2024-01-01\",\"periodCode\":\"MORNING\",\"feedingStatus\":\"NORMAL\",\"hygieneStatus\":\"CLEAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
