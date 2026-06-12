package com.astral.express.pccms.user.dto.response;

import com.astral.express.pccms.user.entity.UserStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    String roleCode,
    OffsetDateTime createdAt,
    UserStatus statusCode
) {}
