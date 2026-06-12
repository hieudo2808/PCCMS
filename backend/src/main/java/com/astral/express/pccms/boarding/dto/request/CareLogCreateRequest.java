package com.astral.express.pccms.boarding.dto.request;

import com.astral.express.pccms.boarding.entity.CarePeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CareLogCreateRequest(
        @NotNull LocalDate logDate,
        @NotNull CarePeriod periodCode,
        @NotBlank @Size(max = 120) String feedingStatus,
        @NotBlank @Size(max = 120) String hygieneStatus,
        @Size(max = 2000) String healthNote,
        @Size(max = 2000) String staffNote,
        @Size(max = 500) String caption
) {
}
