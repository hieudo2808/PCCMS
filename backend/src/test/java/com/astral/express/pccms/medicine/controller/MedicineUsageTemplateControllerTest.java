package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicine.dto.request.CreateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.dto.request.UpdateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.service.MedicineUsageTemplateService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MedicineUsageTemplateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicineUsageTemplateService service;

    @InjectMocks
    private MedicineUsageTemplateController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listByMedicine_success() throws Exception {
        UUID medicineId = UUID.randomUUID();
        given(service.listByMedicine(medicineId)).willReturn(List.of());

        mockMvc.perform(get("/v1/medicines/{medicineId}/usage-templates", medicineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createTemplate_success() throws Exception {
        UUID medicineId = UUID.randomUUID();
        given(service.createTemplate(eq(medicineId), any(CreateMedicineUsageTemplateRequest.class))).willReturn(null);

        mockMvc.perform(post("/v1/medicines/{medicineId}/usage-templates", medicineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Label\",\"dosage\":\"1\",\"frequency\":\"Daily\",\"durationDays\":7,\"instruction\":\"After meal\",\"isDefault\":true,\"sortOrder\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateTemplate_success() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        given(service.updateTemplate(eq(medicineId), eq(templateId), any(UpdateMedicineUsageTemplateRequest.class))).willReturn(null);

        mockMvc.perform(put("/v1/medicines/{medicineId}/usage-templates/{templateId}", medicineId, templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Label 2\",\"dosage\":\"2\",\"frequency\":\"Weekly\",\"durationDays\":14,\"instruction\":\"Before meal\",\"isDefault\":false,\"sortOrder\":2,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteTemplate_success() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        mockMvc.perform(delete("/v1/medicines/{medicineId}/usage-templates/{templateId}", medicineId, templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
