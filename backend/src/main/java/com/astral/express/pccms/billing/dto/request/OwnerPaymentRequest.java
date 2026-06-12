package com.astral.express.pccms.billing.dto.request;

import com.astral.express.pccms.billing.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OwnerPaymentRequest(
        @NotNull @Min(1) Long amountVnd,
        @NotNull PaymentMethod methodCode,
        @Size(max = 120) String referenceCode,
        @Size(max = 2000) String note,
        UUID proofFileId
) {
}
