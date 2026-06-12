package com.astral.express.pccms.boarding.dto.response;

import java.util.UUID;

public record CareLogMediaResponse(
        UUID id,
        UUID fileId,
        String url,
        String mimeType,
        String caption
) {
}
