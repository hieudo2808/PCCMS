package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.catalog.service.ServiceCatalogService;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private com.astral.express.pccms.catalog.service.ServiceCatalogAdminService serviceCatalogAdminService;

    @InjectMocks
    private ServiceCatalogController serviceCatalogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceCatalogController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnValidationFailed_when_TC_SVC_008_missingRequiredFields() throws Exception {
        String request = """
                {
                  "serviceCode": "",
                  "name": "",
                  "categoryCode": null,
                  "basePriceVnd": null
                }
                """;

        mockMvc.perform(post("/v1/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnValidationFailed_when_TC_SVC_010_invalidCategory() throws Exception {
        String request = """
                {
                  "serviceCode": "SVC-BAD-CAT",
                  "name": "Bad Category",
                  "categoryCode": "INVALID",
                  "basePriceVnd": 100000
                }
                """;

        mockMvc.perform(post("/v1/catalog/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_DeleteService_when_DeleteEndpointCalled() throws Exception {
        UUID serviceId = UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/v1/catalog/services/{serviceId}", serviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(serviceCatalogAdminService).delete(serviceId);
    }
}

