package com.astral.express.pccms.user.dto.response;

import com.astral.express.pccms.user.entity.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        String roleCode,
        String roleName,
        List<String> roles,
        UserStatus statusCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
