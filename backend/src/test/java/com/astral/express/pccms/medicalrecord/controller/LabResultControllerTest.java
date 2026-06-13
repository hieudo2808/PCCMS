package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicalrecord.dto.request.CreateLabResultRequest;
import com.astral.express.pccms.medicalrecord.service.LabResultService;
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
class LabResultControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LabResultService labResultService;

    @InjectMocks
    private LabResultController labResultController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(labResultController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listLabResults_success() throws Exception {
        UUID recordId = UUID.randomUUID();
        given(labResultService.listLabResults(recordId)).willReturn(List.of());

        mockMvc.perform(get("/v1/medical-records/{medicalRecordId}/lab-results", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createLabResult_success() throws Exception {
        UUID recordId = UUID.randomUUID();
        given(labResultService.createLabResult(eq(recordId), any(CreateLabResultRequest.class))).willReturn(null);

        mockMvc.perform(post("/v1/medical-records/{medicalRecordId}/lab-results", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testName\":\"Blood Test\",\"resultValue\":\"Normal\",\"referenceRange\":\"1-10\",\"notes\":\"All good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
