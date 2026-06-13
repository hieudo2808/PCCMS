package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.service.BoardingTrackingService;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.security.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingTrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingTrackingService boardingTrackingService;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private BoardingTrackingController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listActiveStays_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        
        mockMvc.perform(get("/v1/boarding/owner/stays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listCareLogs_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        
        mockMvc.perform(get("/v1/boarding/owner/care-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
