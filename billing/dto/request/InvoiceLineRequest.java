package com.astral.express.pccms.billing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineRequest(
        UUID serviceOrderId,
        UUID medicineId,
        @NotBlank(message = "Mô tả không được để trống") String description,
        @NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity,
        @NotNull(message = "Đơn giá không được để trống") @Min(value = 0, message = "Đơn giá không được âm") Long unitPriceVnd
) {
}
