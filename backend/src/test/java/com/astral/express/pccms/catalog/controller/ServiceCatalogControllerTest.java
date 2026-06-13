package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.catalog.dto.request.CreateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.catalog.service.ServiceCatalogAdminService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ServiceCatalogAdminService serviceCatalogAdminService;

    @InjectMocks
    private ServiceCatalogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_success() throws Exception {
        mockMvc.perform(post("/v1/catalog/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"CODE\",\"name\":\"Service\",\"categoryCode\":\"MEDICAL\",\"basePriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void update_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/catalog/services/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"CODE\",\"name\":\"Service\",\"categoryCode\":\"MEDICAL\",\"basePriceVnd\":100,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listCategories_success() throws Exception {
        given(serviceCatalogAdminService.listCategories()).willReturn(List.of());

        mockMvc.perform(get("/v1/catalog/services/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void list_success() throws Exception {
        PageResponse<ServiceCatalogResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(serviceCatalogAdminService.list(any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/catalog/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/v1/catalog/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/catalog/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
