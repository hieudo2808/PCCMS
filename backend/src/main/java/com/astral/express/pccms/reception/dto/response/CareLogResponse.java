package com.astral.express.pccms.reception.dto.response;

import java.util.UUID;

public record CareLogResponse(
        UUID id,
        UUID sessionId,
        UUID petId,
        String petName,
        String staffName,
        Object logDate,
        String periodCode,
        String feedingStatus,
        String hygieneStatus,
        String healthNote,
        String staffNote,
        Object createdAt,
        Boolean canEdit,
        Boolean canDelete,
        Object lockedAt,
        String lockedReason
) {}
