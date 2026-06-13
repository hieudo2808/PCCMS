package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OwnerMedicalRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicalRecordService medicalRecordService;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private OwnerMedicalRecordController ownerMedicalRecordController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerMedicalRecordController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOwnerMedicalRecords_success() throws Exception {
        UUID petId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(medicalRecordService.getOwnerMedicalRecords(eq(petId), eq(userId))).willReturn(List.of());

        mockMvc.perform(get("/v1/owner/pets/{petId}/medical-records", petId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
