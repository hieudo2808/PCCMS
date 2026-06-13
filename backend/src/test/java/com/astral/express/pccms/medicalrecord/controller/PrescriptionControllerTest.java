package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.dto.response.PrescriptionResponse;
import com.astral.express.pccms.medicalrecord.service.PrescriptionService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PrescriptionController prescriptionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(prescriptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_Return201_when_CreatePrescriptionSuccessfully() throws Exception {
        UUID recordId = UUID.randomUUID();
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                UUID.randomUUID(),
                "Take care",
                List.of(new PrescriptionItemRequest(UUID.randomUUID(), "1 per day", 5, "After meal"))
        );
        PrescriptionResponse response = new PrescriptionResponse(
                UUID.randomUUID(),
                "PRE-001",
                recordId,
                request.vetId(),
                request.note(),
                OffsetDateTime.now(),
                List.of()
        );

        given(prescriptionService.createPrescription(eq(recordId), any(CreatePrescriptionRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/v1/medical-records/{id}/prescriptions", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.prescriptionCode").value("PRE-001"));
    }

    @Test
    void should_Return400_when_RequestBodyIsInvalid() throws Exception {
        UUID recordId = UUID.randomUUID();
        String requestJson = """
                {
                  "vetId": "%s",
                  "note": "Take care"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/v1/medical-records/{id}/prescriptions", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_Return200_when_ListPrescriptionsSuccessfully() throws Exception {
        UUID recordId = UUID.randomUUID();
        given(prescriptionService.listPrescriptions(recordId)).willReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/medical-records/{id}/prescriptions", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
