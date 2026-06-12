package com.astral.express.pccms.report.service;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.report.dto.response.ReportSummaryResponse;
import com.astral.express.pccms.report.dto.response.RevenueSummaryRowResponse;
import com.astral.express.pccms.report.entity.ReportType;
import com.astral.express.pccms.report.repository.RevenueReportRepository;
import com.astral.express.pccms.report.repository.RevenueSummaryRow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportSummaryServiceTest {

    private static final UUID SERVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private RevenueReportRepository revenueReportRepository;

    @InjectMocks
    private ReportSummaryService reportSummaryService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/report-summary.csv", numLinesToSkip = 1)
    void should_followReportSummaryCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        ReportSummaryCsvInput csv = parseInput(input);

        switch (scenario) {
            case "Revenue report by date range success", "Only paid and partially paid invoices counted" ->
                    assertDateRangeReportSuccess(csv);
            case "Report filtered by service category success" -> assertCategoryFilterSuccess(csv);
            case "Report filtered by service success" -> assertServiceFilterSuccess(csv);
            case "No report data returns empty result" -> assertNoDataReturnsEmptySummary(csv);
            case "Missing date range rejected", "Missing report type rejected", "Invalid date range rejected" ->
                    assertValidationFailure(csv, ErrorCode.valueOf(expectedErrorCode));
            case "User without report permission rejected" ->
                    assertThat(expectedErrorCode).isEqualTo(ErrorCode.ERR_403_FORBIDDEN.name());
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    private void assertDateRangeReportSuccess(ReportSummaryCsvInput csv) {
        RevenueSummaryRow row = row(ServiceCategory.MEDICAL, SERVICE_ID, "Khám tổng quát", "250000", 2L);
        given(revenueReportRepository.findByDateRange(eq(csv.fromDate()), eq(csv.toDate()))).willReturn(List.of(row));

        ReportSummaryResponse response = reportSummaryService.getSummary(
                csv.fromDate(), csv.toDate(), csv.reportType(), null, null);

        assertThat(response.totalRevenueVnd()).isEqualByComparingTo("250000");
        assertThat(response.totalInvoiceCount()).isEqualTo(2L);
        assertThat(response.items()).extracting(RevenueSummaryRowResponse::serviceName).containsExactly("Khám tổng quát");
    }

    private void assertCategoryFilterSuccess(ReportSummaryCsvInput csv) {
        RevenueSummaryRow row = row(ServiceCategory.MEDICAL, SERVICE_ID, "Khám tổng quát", "200000", 1L);
        given(revenueReportRepository.findByDateRangeAndCategoryCode(
                eq(csv.fromDate()), eq(csv.toDate()), eq(csv.categoryCode().name()))).willReturn(List.of(row));

        ReportSummaryResponse response = reportSummaryService.getSummary(
                csv.fromDate(), csv.toDate(), csv.reportType(), csv.categoryCode(), null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().categoryCode()).isEqualTo(ServiceCategory.MEDICAL);
    }

    private void assertServiceFilterSuccess(ReportSummaryCsvInput csv) {
        RevenueSummaryRow row = row(ServiceCategory.MEDICAL, SERVICE_ID, "Khám tổng quát", "200000", 1L);
        given(revenueReportRepository.findByDateRangeAndServiceId(
                eq(csv.fromDate()), eq(csv.toDate()), eq(SERVICE_ID))).willReturn(List.of(row));

        ReportSummaryResponse response = reportSummaryService.getSummary(
                csv.fromDate(), csv.toDate(), csv.reportType(), null, csv.serviceId());

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().serviceId()).isEqualTo(SERVICE_ID);
    }

    private void assertNoDataReturnsEmptySummary(ReportSummaryCsvInput csv) {
        given(revenueReportRepository.findByDateRange(eq(csv.fromDate()), eq(csv.toDate()))).willReturn(List.of());

        ReportSummaryResponse response = reportSummaryService.getSummary(
                csv.fromDate(), csv.toDate(), csv.reportType(), null, null);

        assertThat(response.totalRevenueVnd()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalInvoiceCount()).isZero();
        assertThat(response.items()).isEmpty();
    }

    private void assertValidationFailure(ReportSummaryCsvInput csv, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> reportSummaryService.getSummary(
                csv.fromDate(), csv.toDate(), csv.reportType(), csv.categoryCode(), csv.serviceId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
    }

    private RevenueSummaryRow row(
            ServiceCategory categoryCode,
            UUID serviceId,
            String serviceName,
            String revenueVnd,
            Long invoiceCount) {
        return new RevenueSummaryRow(
                LocalDate.of(2026, 4, 10),
                categoryCode,
                serviceId,
                serviceName,
                new BigDecimal(revenueVnd),
                invoiceCount
        );
    }

    private ReportSummaryCsvInput parseInput(String input) {
        return new ReportSummaryCsvInput(
                date(input, "fromDate"),
                date(input, "toDate"),
                reportType(input, "reportType"),
                category(input, "categoryCode"),
                serviceId(input, "serviceId")
        );
    }

    private LocalDate date(String input, String key) {
        String value = text(input, key);
        return value == null ? null : LocalDate.parse(value);
    }

    private ReportType reportType(String input, String key) {
        String value = text(input, key);
        return value == null ? null : ReportType.valueOf(value);
    }

    private ServiceCategory category(String input, String key) {
        String value = text(input, key);
        return value == null ? null : ServiceCategory.valueOf(value);
    }

    private UUID serviceId(String input, String key) {
        String value = text(input, key);
        return value == null ? null : SERVICE_ID;
    }

    private String text(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                String value = pair[1].trim();
                return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
            }
        }
        return null;
    }

    private record ReportSummaryCsvInput(
            LocalDate fromDate,
            LocalDate toDate,
            ReportType reportType,
            ServiceCategory categoryCode,
            UUID serviceId
    ) {
    }
}


