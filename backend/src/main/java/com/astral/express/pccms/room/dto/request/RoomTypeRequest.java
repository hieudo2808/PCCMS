package com.astral.express.pccms.room.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomTypeRequest(
        @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) Integer defaultCapacity,
        @NotNull @Min(0) Long baseDailyPriceVnd,
        String description,
        Boolean isActive
) {
}
