package com.astral.express.pccms.billing.dto.response;

import com.astral.express.pccms.billing.entity.PaymentMethod;
import com.astral.express.pccms.billing.entity.PaymentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentCode,
        UUID invoiceId,
        Long amountVnd,
        PaymentMethod methodCode,
        PaymentStatus statusCode,
        OffsetDateTime paidAt,
        UUID receivedBy,
        String note
) {
}
