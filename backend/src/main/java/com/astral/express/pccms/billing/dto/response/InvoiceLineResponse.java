package com.astral.express.pccms.billing.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineResponse(
        UUID id,
        UUID invoiceId,
        UUID serviceOrderId,
        UUID medicineId,
        String description,
        Integer quantity,
        Long unitPriceVnd,
        Long totalAmountVnd
) {
}
