package com.astral.express.pccms.reception.dto.response;

import java.util.UUID;

public record CareLogMediaResponse(
        UUID id,
        String originalName,
        String storedKey,
        String mimeType,
        Long sizeBytes,
        UUID uploadedBy,
        String visibilityCode
) {}
