package com.astral.express.pccms.report.repository;

import com.astral.express.pccms.appointment.entity.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RevenueSummaryRow(
        LocalDate reportDate,
        ServiceCategory categoryCode,
        UUID serviceId,
        String serviceName,
        BigDecimal revenueVnd,
        Long invoiceCount
) {
}

