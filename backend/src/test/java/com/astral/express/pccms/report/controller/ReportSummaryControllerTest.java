package com.astral.express.pccms.report.controller;

import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.report.entity.ReportType;
import com.astral.express.pccms.report.service.ReportSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportSummaryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportSummaryService reportSummaryService;

    @InjectMocks
    private ReportSummaryController reportSummaryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportSummaryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnValidationFailed_when_TC_RPT_004_missingDateRange() throws Exception {
        given(reportSummaryService.getSummary(null, null, ReportType.REVENUE, null, null))
                .willThrow(new BusinessException(ErrorCode.ERR_400_BAD_REQUEST));

        mockMvc.perform(get("/v1/admin/reports/summary")
                        .param("reportType", "REVENUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnValidationFailed_when_invalidDateBinding() throws Exception {
        mockMvc.perform(get("/v1/admin/reports/summary")
                        .param("fromDate", "not-a-date")
                        .param("toDate", "2026-04-30")
                        .param("reportType", "REVENUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_RequireReportViewPermission_when_TC_RPT_009_accessingSummaryEndpoint() throws Exception {
        Method method = ReportSummaryController.class.getMethod(
                "getSummary",
                LocalDate.class,
                LocalDate.class,
                com.astral.express.pccms.report.entity.ReportType.class,
                com.astral.express.pccms.appointment.entity.ServiceCategory.class,
                java.util.UUID.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAuthority('REPORT_VIEW')");
    }
}

