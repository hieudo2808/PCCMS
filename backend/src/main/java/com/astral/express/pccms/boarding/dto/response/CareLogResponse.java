package com.astral.express.pccms.boarding.dto.response;

import com.astral.express.pccms.boarding.entity.CarePeriod;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CareLogResponse(
        UUID id,
        UUID sessionId,
        LocalDate logDate,
        CarePeriod periodCode,
        String feedingStatus,
        String hygieneStatus,
        String healthNote,
        String staffNote,
        UUID staffId,
        String staffName,
        OffsetDateTime createdAt,
        List<CareLogMediaResponse> media
) {
}
