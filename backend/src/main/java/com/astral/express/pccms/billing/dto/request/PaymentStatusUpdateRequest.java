package com.astral.express.pccms.billing.dto.request;

import com.astral.express.pccms.billing.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentStatusUpdateRequest(
        @NotNull PaymentStatus statusCode,
        @Size(max = 2000) String note
) {
}
