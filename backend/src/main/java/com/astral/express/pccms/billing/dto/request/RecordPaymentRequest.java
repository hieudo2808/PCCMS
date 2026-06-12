package com.astral.express.pccms.billing.dto.request;

import com.astral.express.pccms.billing.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecordPaymentRequest(
        @NotNull UUID invoiceId,
        @NotNull @Min(1) Long amountVnd,
        @NotNull PaymentMethod methodCode,
        OffsetDateTime paidAt,
        @Size(max = 2000) String note
) {
}
