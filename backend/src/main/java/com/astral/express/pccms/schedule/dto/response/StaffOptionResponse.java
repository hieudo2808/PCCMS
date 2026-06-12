package com.astral.express.pccms.schedule.dto.response;

import java.util.UUID;

public record StaffOptionResponse(
        UUID id,
        String fullName,
        String roleCode,
        String roleName
) {
}
