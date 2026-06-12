package com.astral.express.pccms.reception.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroomingStatusUpdateRequest(
        @NotBlank(message = "Vui lòng chọn trạng thái mới")
        @Pattern(regexp = "PENDING|CONFIRMED|IN_SERVICE|COMPLETED|CANCELLED", message = "Trạng thái dịch vụ làm đẹp không hợp lệ")
        String statusCode,
        @Size(max = 500)
        String internalNote
) {}
