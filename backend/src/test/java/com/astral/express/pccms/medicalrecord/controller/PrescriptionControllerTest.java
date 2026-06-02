package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
    void should_Return200_when_CreatePrescriptionSuccessfully() throws Exception {
        UUID recordId = UUID.randomUUID();
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                UUID.randomUUID(),
                "Take care",
                List.of(new PrescriptionItemRequest(UUID.randomUUID(), "1 per day", 5, "After meal"))
        );

        doNothing().when(prescriptionService).createPrescription(eq(recordId), any(CreatePrescriptionRequest.class));

        mockMvc.perform(post("/api/v1/medical-records/{id}/prescriptions", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"));
    }

    @Test
    void should_Return400_when_RequestBodyIsInvalid() throws Exception {
        UUID recordId = UUID.randomUUID();
        
        // Invalid request: items list is null or empty
        String requestJson = """
                {
                  "vetId": "%s",
                  "note": "Take care"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/medical-records/{id}/prescriptions", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
