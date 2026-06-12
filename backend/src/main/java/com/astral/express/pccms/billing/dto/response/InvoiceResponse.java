package com.astral.express.pccms.billing.dto.response;

import com.astral.express.pccms.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceCode,
        UUID ownerId,
        UUID petId,
        InvoiceStatus statusCode,
        Long totalAmountVnd,
        Long paidAmountVnd,
        OffsetDateTime issuedAt,
        String note,
        List<InvoiceLineResponse> lines
) {
}
