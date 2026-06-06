package com.astral.express.pccms.grooming.dto.response;

import com.astral.express.pccms.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GroomingInvoiceSummaryResponse(
        UUID id,
        String invoiceCode,
        InvoiceStatus statusCode,
        BigDecimal totalAmountVnd,
        BigDecimal paidAmountVnd,
        OffsetDateTime issuedAt
) {
}
