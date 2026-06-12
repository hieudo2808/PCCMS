package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;
import com.astral.express.pccms.medicine.service.MedicineCategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MedicineCategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicineCategoryService medicineCategoryService;

    @InjectMocks
    private MedicineCategoryController medicineCategoryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String BASE_URL = "/api/v1/catalog/medicine-categories";
    private final String CONTEXT_PATH = "/api";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(medicineCategoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 1. API-MEDCAT-001: should_ListActiveCategories_Successfully
    @Test
    @DisplayName("API-MEDCAT-001: List medicine categories (activeOnly=true)")
    void should_ListActiveCategories_Successfully() throws Exception {
        UUID id1 = UUID.randomUUID();
        MedicineCategoryResponse response1 = new MedicineCategoryResponse(id1, "Category 1", "Desc 1", true);
        
        when(medicineCategoryService.listActive()).thenReturn(List.of(response1));

        mockMvc.perform(get(BASE_URL)
                .contextPath(CONTEXT_PATH)
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data[0].id").value(id1.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Category 1"))
                .andExpect(jsonPath("$.data[0].description").value("Desc 1"))
                .andExpect(jsonPath("$.data[0].isActive").value(true));

        verify(medicineCategoryService).listActive();
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 2. API-MEDCAT-002: should_ListAllCategories_When_ActiveOnlyIsFalse
    @Test
    @DisplayName("API-MEDCAT-002: List medicine categories (activeOnly=false)")
    void should_ListAllCategories_When_ActiveOnlyIsFalse() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        MedicineCategoryResponse response1 = new MedicineCategoryResponse(id1, "Category 1", "Desc 1", true);
        MedicineCategoryResponse response2 = new MedicineCategoryResponse(id2, "Category 2", "Desc 2", false);
        
        when(medicineCategoryService.listAll()).thenReturn(List.of(response1, response2));

        mockMvc.perform(get(BASE_URL)
                .contextPath(CONTEXT_PATH)
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].id").value(id2.toString()))
                .andExpect(jsonPath("$.data[1].isActive").value(false));

        verify(medicineCategoryService).listAll();
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 3. API-MEDCAT-003: should_GetCategoryById_Successfully
    @Test
    @DisplayName("API-MEDCAT-003: Get medicine category by ID successfully")
    void should_GetCategoryById_Successfully() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineCategoryResponse response = new MedicineCategoryResponse(id, "Category 1", "Desc 1", true);
        
        when(medicineCategoryService.getById(id)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Category 1"))
                .andExpect(jsonPath("$.data.description").value("Desc 1"))
                .andExpect(jsonPath("$.data.isActive").value(true));

        verify(medicineCategoryService).getById(id);
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 4. API-MEDCAT-004: should_Return404_When_CategoryNotFound
    @Test
    @DisplayName("API-MEDCAT-004: Get medicine category - Not Found")
    void should_Return404_When_CategoryNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        
        when(medicineCategoryService.getById(id))
                .thenThrow(new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND.name()));

        verify(medicineCategoryService).getById(id);
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 5. API-MEDCAT-005: should_CreateCategory_Successfully
    @Test
    @DisplayName("API-MEDCAT-005: Create medicine category successfully")
    void should_CreateCategory_Successfully() throws Exception {
        MedicineCategoryCreateRequest request = new MedicineCategoryCreateRequest("Category 1", "Desc 1", true);
        UUID id = UUID.randomUUID();
        MedicineCategoryResponse response = new MedicineCategoryResponse(id, "Category 1", "Desc 1", true);
        
        when(medicineCategoryService.create(any(MedicineCategoryCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())  // API-MEDCAT-005: project convention — ApiResponse.created() returns HTTP 200; body code=201
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Category 1"))
                .andExpect(jsonPath("$.data.description").value("Desc 1"))
                .andExpect(jsonPath("$.data.isActive").value(true));

        verify(medicineCategoryService).create(any(MedicineCategoryCreateRequest.class));
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 6. API-MEDCAT-006: should_Return409_When_CategoryNameExists
    @Test
    @DisplayName("API-MEDCAT-006: Create medicine category - Name already exists")
    void should_Return409_When_CategoryNameExists() throws Exception {
        MedicineCategoryCreateRequest request = new MedicineCategoryCreateRequest("Category 1", "Desc 1", true);
        
        when(medicineCategoryService.create(any(MedicineCategoryCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS));

        mockMvc.perform(post(BASE_URL)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS.name()));

        verify(medicineCategoryService).create(any(MedicineCategoryCreateRequest.class));
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 7. API-MEDCAT-007: should_Return400_When_CreateRequestIsInvalid
    @ParameterizedTest(name = "API-MEDCAT-007: Create validation failure - Case {1}")
    @CsvFileSource(resources = "/testcases/medicine-category-api-tests.csv", numLinesToSkip = 1)
    void should_Return400_When_CreateRequestIsInvalid(
            String ruleId, String caseId, String action, String name, String description, Boolean isActive,
            int expectedStatus, String expectedErrorCode) throws Exception {
        
        if (!"CREATE".equals(action)) return;

        // Construct invalid JSON
        String json = String.format("{\"name\": %s, \"description\": %s, \"isActive\": %s}",
                name == null ? "null" : "\"" + name + "\"",
                description == null ? "null" : "\"" + description + "\"",
                isActive == null ? "null" : isActive.toString());

        mockMvc.perform(post(BASE_URL)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        verifyNoInteractions(medicineCategoryService);
    }

    // 8. API-MEDCAT-008: should_UpdateCategory_Successfully
    @Test
    @DisplayName("API-MEDCAT-008: Update medicine category successfully")
    void should_UpdateCategory_Successfully() throws Exception {
        UUID id = UUID.randomUUID();
        MedicineCategoryUpdateRequest request = new MedicineCategoryUpdateRequest("Updated Cat", "Updated Desc", false);
        MedicineCategoryResponse response = new MedicineCategoryResponse(id, "Updated Cat", "Updated Desc", false);
        
        when(medicineCategoryService.update(eq(id), any(MedicineCategoryUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Updated Cat"))
                .andExpect(jsonPath("$.data.description").value("Updated Desc"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        verify(medicineCategoryService).update(eq(id), any(MedicineCategoryUpdateRequest.class));
        verifyNoMoreInteractions(medicineCategoryService);
    }

    // 9. API-MEDCAT-009: should_Return400_When_UpdateRequestIsInvalid
    @ParameterizedTest(name = "API-MEDCAT-009: Update validation failure - Case {1}")
    @CsvFileSource(resources = "/testcases/medicine-category-api-tests.csv", numLinesToSkip = 1)
    void should_Return400_When_UpdateRequestIsInvalid(
            String ruleId, String caseId, String action, String name, String description, Boolean isActive,
            int expectedStatus, String expectedErrorCode) throws Exception {
        
        if (!"UPDATE".equals(action)) return;

        UUID id = UUID.randomUUID();

        // Construct invalid JSON
        String json = String.format("{\"name\": %s, \"description\": %s, \"isActive\": %s}",
                name == null ? "null" : "\"" + name + "\"",
                description == null ? "null" : "\"" + description + "\"",
                isActive == null ? "null" : isActive.toString());

        mockMvc.perform(put(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        verifyNoInteractions(medicineCategoryService);
    }

    // 10. API-MEDCAT-010: should_DeleteCategory_Successfully
    @Test
    @DisplayName("API-MEDCAT-010: Delete medicine category successfully")
    void should_DeleteCategory_Successfully() throws Exception {
        UUID id = UUID.randomUUID();
        
        doNothing().when(medicineCategoryService).delete(id);

        mockMvc.perform(delete(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Thao tác thành công"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(medicineCategoryService).delete(id);
        verifyNoMoreInteractions(medicineCategoryService);
    }

    @Test
    @DisplayName("API-MEDCAT-011: Delete medicine category - In Use")
    void should_Return409_When_CategoryInUse() throws Exception {
        UUID id = UUID.randomUUID();
        
        doThrow(new BusinessException(ErrorCode.ERR_MED_010_CATEGORY_IN_USE))
                .when(medicineCategoryService).delete(id);

        mockMvc.perform(delete(BASE_URL + "/{id}", id)
                .contextPath(CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_MED_010_CATEGORY_IN_USE.name()));

        verify(medicineCategoryService).delete(id);
        verifyNoMoreInteractions(medicineCategoryService);
    }
}
