package com.astral.express.pccms.user.dto.response;

import com.astral.express.pccms.user.entity.UserStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
    UUID userId,
    String fullName,
    String email,
    String avatarUrl,
    String bio,
    String roleName,
    OffsetDateTime createdAt,
    UserStatus statusCode
) {}
