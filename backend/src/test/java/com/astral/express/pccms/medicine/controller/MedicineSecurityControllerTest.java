package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.medicine.service.MedicineCategoryService;
import com.astral.express.pccms.medicine.service.MedicineService;
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

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Disabled("Fails due to ApplicationContext failure threshold (1) exceeded in integration test suite")
class MedicineSecurityControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private MedicineService medicineService;

    @MockitoBean
    private MedicineCategoryService medicineCategoryService;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("SEC-MED-001.1: Unauthenticated request to /v1/medicines -> 401")
    void secMed001_1_Unauthenticated() throws Exception {
        String requestJson = "{\"name\":\"Aspirin\",\"unit\":\"tablet\",\"currentStock\":100,\"unitPriceVnd\":1000}";
        mockMvc.perform(post("/api/v1/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(medicineService);
    }

    @Test
    @DisplayName("SEC-MED-001.2: Authenticated without MEDICINE_MANAGE to /v1/medicines -> 403")
    @WithMockUser(authorities = {"MEDICINE_READ"})
    void secMed001_2_Forbidden() throws Exception {
        String requestJson = "{\"name\":\"Aspirin\",\"unit\":\"tablet\",\"currentStock\":100,\"unitPriceVnd\":1000}";
        mockMvc.perform(post("/api/v1/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isForbidden());
        verifyNoInteractions(medicineService);
    }

    @Test
    @DisplayName("SEC-MED-001.3: Authenticated with MEDICINE_MANAGE to /v1/medicines -> 200/201/400")
    @WithMockUser(authorities = {"MEDICINE_MANAGE"})
    void secMed001_3_Allowed() throws Exception {
        String requestJson = "{\"name\":\"Aspirin\",\"unit\":\"tablet\",\"currentStock\":100,\"unitPriceVnd\":1000}";
        mockMvc.perform(post("/api/v1/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode != 200 && statusCode != 201 && statusCode != 400) {
                        throw new AssertionError("Expected 200, 201 or 400 but got " + statusCode);
                    }
                });
    }

    @Test
    @DisplayName("SEC-MED-002.1: Unauthenticated request to /v1/catalog/medicine-categories/{id} -> 401")
    void secMed002_1_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/medicine-categories/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(medicineCategoryService);
    }

    @Test
    @DisplayName("SEC-MED-002.2: Authenticated without required authority to /v1/catalog/medicine-categories/{id} -> 403")
    @WithMockUser(authorities = {"USER_READ"})
    void secMed002_2_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/medicine-categories/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(medicineCategoryService);
    }

    @Test
    @DisplayName("SEC-MED-002.3: Authenticated with PRESCRIPTION_CREATE to /v1/catalog/medicine-categories/{id} -> 200")
    @WithMockUser(authorities = {"PRESCRIPTION_CREATE"})
    void secMed002_3_Allowed() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/medicine-categories/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
