package com.astral.express.pccms.schedule.dto.response;

import java.util.UUID;

public record RoleOptionResponse(
        UUID id,
        String code,
        String name
) {
}
