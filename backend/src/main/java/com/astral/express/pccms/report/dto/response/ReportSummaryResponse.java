package com.astral.express.pccms.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReportSummaryResponse(
        BigDecimal totalRevenueVnd,
        Long totalInvoiceCount,
        List<RevenueSummaryRowResponse> items
) {
}
