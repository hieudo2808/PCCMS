package com.astral.express.pccms.boarding.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CareLogResponse(
        UUID id,
        UUID petId,
        String petName,
        LocalDate logDate,
        String periodCode,
        String periodLabel,
        String feedingStatus,
        String hygieneStatus,
        String healthNote,
        String staffNote,
        List<String> mediaCaptions
) {}
