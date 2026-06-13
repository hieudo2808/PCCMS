package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.service.MedicineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicineService medicineService;

    @InjectMocks
    private MedicineController medicineController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(medicineController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnCreatedMedicine_when_ValidRequest() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineCreateRequest request = new MedicineCreateRequest("CODE1", "Medicine 1", null, "Box", null, 10, 100L);
        MedicineResponse response = new MedicineResponse(id, "CODE1", "Medicine 1", null, null, "Box", null, 10, 100L, true);

        given(medicineService.createMedicine(any(MedicineCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.medicineCode").value("CODE1"));
    }

    @Test
    void should_ReturnBadRequest_when_CreateWithInvalidData() throws Exception {
        // Missing name and unit
        MedicineCreateRequest request = new MedicineCreateRequest("CODE1", "", null, "", null, -10, Long.valueOf(-100));

        mockMvc.perform(post("/v1/medicines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_ReturnUpdatedMedicine_when_ValidRequest() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineUpdateRequest request = new MedicineUpdateRequest(
                "MED002", "Updated Name", UUID.randomUUID(), "Bottle", "Take 2", 100, 50000L);
        MedicineResponse response = new MedicineResponse(id, "CODE1", "Medicine Updated", null, null, "Box", null, 10, 100L, true);

        given(medicineService.updateMedicine(eq(id), any(MedicineUpdateRequest.class))).willReturn(response);

        mockMvc.perform(put("/v1/medicines/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Medicine Updated"));
    }

    @Test
    void should_ReturnMedicine_when_GetById() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineResponse response = new MedicineResponse(id, "CODE1", "Medicine 1", null, null, "Box", null, 10, 100L, true);

        given(medicineService.getMedicine(id)).willReturn(response);

        mockMvc.perform(get("/v1/medicines/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void should_ReturnPageOfMedicines_when_GetAll() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineResponse response = new MedicineResponse(id, "CODE1", "Medicine 1", null, null, "Box", null, 10, 100L, true);
        PageResponse<MedicineResponse> pageResponse = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of(response)));

        given(medicineService.getAllMedicines(any(), any(Pageable.class))).willReturn(pageResponse);

        mockMvc.perform(get("/v1/medicines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnSuccess_when_AddStock() throws Exception {
        UUID id = UUID.randomUUID();
        AddStockRequest request = new AddStockRequest(5);
        MedicineResponse response = new MedicineResponse(id, "CODE1", "Medicine 1", null, null, "Box", null, 15, 100L, true);

        given(medicineService.addStock(eq(id), any(AddStockRequest.class))).willReturn(response);

        mockMvc.perform(patch("/v1/medicines/{id}/stock", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStock").value(15));
    }

    @Test
    void should_ReturnSuccess_when_DeleteMedicine() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v1/medicines/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnCategories_when_ListCategories() throws Exception {
        given(medicineService.listCategories()).willReturn(List.of());

        mockMvc.perform(get("/v1/medicines/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnPageOfMedicines_when_GetAllWithKeyword() throws Exception {
        PageResponse<MedicineResponse> pageResponse = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(medicineService.searchMedicines(any(), any(), any(), any())).willReturn(pageResponse);

        mockMvc.perform(get("/v1/medicines")
                .param("keyword", "A")
                .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnPageOfMedicines_when_SuggestMedicines() throws Exception {
        PageResponse<MedicineResponse> pageResponse = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(medicineService.searchMedicines(any(), any(), any(), any())).willReturn(pageResponse);

        mockMvc.perform(get("/v1/medicines/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_ReturnPageOfMedicines_when_GetAllWithIsActiveOnly() throws Exception {
        PageResponse<MedicineResponse> pageResponse = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(medicineService.searchMedicines(any(), any(), any(), any())).willReturn(pageResponse);

        mockMvc.perform(get("/v1/medicines")
                .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
