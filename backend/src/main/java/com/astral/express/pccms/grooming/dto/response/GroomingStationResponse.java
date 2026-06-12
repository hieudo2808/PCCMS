package com.astral.express.pccms.grooming.dto.response;

import java.util.UUID;

public record GroomingStationResponse(
        UUID id,
        String stationCode,
        String name,
        Boolean isActive
) {
}
