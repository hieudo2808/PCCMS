package com.astral.express.pccms.user.dto.request;

import lombok.Builder;

@Builder
public record UserProfileUpdateRequest(
    String fullName,
    String phone
) {}
