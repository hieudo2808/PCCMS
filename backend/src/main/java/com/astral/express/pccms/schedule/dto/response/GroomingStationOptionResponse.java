package com.astral.express.pccms.schedule.dto.response;

import java.util.UUID;

public record GroomingStationOptionResponse(
        UUID id,
        String stationCode,
        String name
) {
}
