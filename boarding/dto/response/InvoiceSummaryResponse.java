package com.astral.express.pccms.boarding.dto.response;

import com.astral.express.pccms.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceSummaryResponse(
        UUID id,
        String invoiceCode,
        InvoiceStatus statusCode,
        Long totalAmountVnd,
        Long paidAmountVnd,
        OffsetDateTime issuedAt
) {
}
