package com.astral.express.pccms.report.service;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.report.dto.response.ReportSummaryResponse;
import com.astral.express.pccms.report.dto.response.RevenueSummaryRowResponse;
import com.astral.express.pccms.report.entity.ReportType;
import com.astral.express.pccms.report.repository.RevenueReportRepository;
import com.astral.express.pccms.report.repository.RevenueSummaryRow;
import com.astral.express.pccms.report.service.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportSummaryService {

    private final RevenueReportRepository revenueReportRepository;
public ReportSummaryResponse getSummary(
            LocalDate fromDate,
            LocalDate toDate,
            ReportType reportType,
            ServiceCategory categoryCode,
            UUID serviceId) {
        validateRequest(fromDate, toDate, reportType);

        List<RevenueSummaryRow> rows = findRevenueRows(fromDate, toDate, categoryCode, serviceId);
        BigDecimal totalRevenue = rows.stream()
                .map(RevenueSummaryRow::revenueVnd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalInvoiceCount = rows.stream()
                .mapToLong(RevenueSummaryRow::invoiceCount)
                .sum();
        List<RevenueSummaryRowResponse> items = rows.stream()
                .map(this::toResponse)
                .toList();

        return new ReportSummaryResponse(totalRevenue, totalInvoiceCount, items);
    }

    private void validateRequest(LocalDate fromDate, LocalDate toDate, ReportType reportType) {
        if (fromDate == null || toDate == null || reportType == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (toDate.isBefore(fromDate)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (reportType != ReportType.REVENUE) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private List<RevenueSummaryRow> findRevenueRows(
            LocalDate fromDate,
            LocalDate toDate,
            ServiceCategory categoryCode,
            UUID serviceId) {
        if (serviceId != null) {
            return revenueReportRepository.findByDateRangeAndServiceId(fromDate, toDate, serviceId);
        }
        if (categoryCode != null) {
            return revenueReportRepository.findByDateRangeAndCategoryCode(fromDate, toDate, categoryCode.name());
        }
        return revenueReportRepository.findByDateRange(fromDate, toDate);
    }

    private RevenueSummaryRowResponse toResponse(RevenueSummaryRow row) {
        return new RevenueSummaryRowResponse(
                row.reportDate(),
                row.categoryCode(),
                row.serviceId(),
                row.serviceName(),
                row.revenueVnd(),
                row.invoiceCount()
        );
    }
}



