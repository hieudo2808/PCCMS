package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.service.MedicalRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicalRecordService medicalRecordService;

    @InjectMocks
    private MedicalRecordController medicalRecordController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(medicalRecordController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
                
        objectMapper = new ObjectMapper();
    }

    @Test
    void should_ReturnUpdatedRecord_when_ValidUpdateRequest() throws Exception {
        UUID recordId = UUID.randomUUID();
        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                BigDecimal.valueOf(38.0), 120, 30, BigDecimal.valueOf(5.0),
                "120/80", 98, "Pink", BigDecimal.valueOf(1.5),
                "Fever", "Rest"
        );

        MedicalRecordResponse mockResponse = new MedicalRecordResponse(
                recordId, "MR-001", null, null, "Pet Name", null, "Vet Name", RecordStatus.DRAFT,
                BigDecimal.valueOf(38.0), 120, 30, BigDecimal.valueOf(5.0),
                "120/80", 98, "Pink", BigDecimal.valueOf(1.5),
                "Fever", null, "Rest", null, null, null, null
        );

        given(medicalRecordService.updateMedicalRecord(eq(recordId), any(UpdateMedicalRecordRequest.class)))
                .willReturn(mockResponse);

        mockMvc.perform(put("/v1/medical-records/{id}", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temperatureC").value(38.0));
    }

    @Test
    void should_ReturnBadRequest_when_UpdateWithInvalidVitals() throws Exception {
        UUID recordId = UUID.randomUUID();
        // Negative temperature is physically impossible and should fail.
        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                BigDecimal.valueOf(-1.0), 120, 30, BigDecimal.valueOf(5.0),
                "120/80", 98, "Pink", BigDecimal.valueOf(1.5),
                "Fever", "Rest"
        );

        mockMvc.perform(put("/v1/medical-records/{id}", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_ReturnFinalizedRecord_when_ValidFinalizeRequest() throws Exception {
        UUID recordId = UUID.randomUUID();
        FinalizeMedicalRecordRequest request = new FinalizeMedicalRecordRequest(
                "Confirmed Infection",
                OffsetDateTime.now().plusDays(7),
                "Continue antibiotics"
        );

        MedicalRecordResponse mockResponse = new MedicalRecordResponse(
                recordId, "MR-001", null, null, "Pet Name", null, "Vet Name", RecordStatus.FINALIZED,
                BigDecimal.valueOf(38.0), 120, 30, BigDecimal.valueOf(5.0),
                "120/80", 98, "Pink", BigDecimal.valueOf(1.5),
                "Fever", "Confirmed Infection", "Continue antibiotics", 
                request.followUpAt(), OffsetDateTime.now(), null, null
        );

        given(medicalRecordService.finalizeMedicalRecord(eq(recordId), any(FinalizeMedicalRecordRequest.class)))
                .willReturn(mockResponse);

        String jsonContent = """
                {
                    "finalDiagnosis": "Confirmed Infection",
                    "followUpAt": "2026-06-09T00:00:00Z",
                    "treatmentNote": "Continue antibiotics"
                }
                """;

        mockMvc.perform(patch("/v1/medical-records/{id}/finalize", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recordStatus").value("FINALIZED"))
                .andExpect(jsonPath("$.data.finalDiagnosis").value("Confirmed Infection"));
    }

    @Test
    void should_ReturnBadRequest_when_FinalizeWithEmptyDiagnosis() throws Exception {
        UUID recordId = UUID.randomUUID();
        String jsonContent = """
                {
                    "finalDiagnosis": "",
                    "followUpAt": "2026-06-09T00:00:00Z",
                    "treatmentNote": "Continue antibiotics"
                }
                """;

        mockMvc.perform(patch("/v1/medical-records/{id}/finalize", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
