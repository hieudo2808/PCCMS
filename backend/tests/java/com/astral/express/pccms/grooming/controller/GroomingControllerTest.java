package com.astral.express.pccms.grooming.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.service.GroomingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GroomingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroomingService groomingService;

    @InjectMocks
    private GroomingController groomingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groomingController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnActiveServices_when_ListServices() throws Exception {
        GroomingServiceResponse response = new GroomingServiceResponse(
                UUID.randomUUID(),
                "GRM-BATH",
                "Tam say",
                null,
                100000L,
                60,
                true);
        given(groomingService.listActiveServices()).willReturn(List.of(response));

        mockMvc.perform(get("/v1/grooming/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].serviceCode").value("GRM-BATH"));
    }

    @Test
    void should_ReturnBadRequest_when_CreateBookingMissingRequiredFields() throws Exception {
        String invalidJson = """
                {
                  "ownerNote": "Can nhe tay"
                }
                """;

        mockMvc.perform(post("/v1/grooming/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_DeclareOwnerOnlyAccess_when_CreateBookingEndpoint() throws Exception {
        Method method = GroomingController.class.getMethod("createBooking", GroomingBookingCreateRequest.class);

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('OWNER')");
    }

    @Test
    void should_DeclareAdminOnlyAccess_when_AdminServiceEndpoint() throws Exception {
        Method method = GroomingController.class.getMethod("listGroomingServicesForAdmin");

        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
