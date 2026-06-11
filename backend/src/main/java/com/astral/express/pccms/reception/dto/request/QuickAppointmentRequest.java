package com.astral.express.pccms.reception.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record QuickAppointmentRequest(
        @NotBlank(message = "Cần nhập SĐT khi tạo nhanh lịch hẹn")
        @Pattern(regexp = "^[0-9 .-]{9,20}$", message = "SĐT không hợp lệ")
        String phone,
        @NotBlank(message = "Vui lòng nhập tên khách")
        @Size(max = 150)
        String ownerName,
        @NotBlank(message = "Vui lòng nhập tên thú cưng")
        @Size(max = 80)
        String petName,
        UUID doctorId,
        String serviceCode,
        String scheduledStartAt,
        String scheduledEndAt,
        @NotBlank(message = "Vui lòng nhập triệu chứng ban đầu")
        @Size(max = 500)
        String symptomText,
        @Size(max = 255)
        String ownerNote
) {}
