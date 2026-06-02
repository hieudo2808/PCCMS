package com.astral.express.pccms.user.dto.request;

import com.astral.express.pccms.user.entity.UserStatus;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record AdminUpdateUserRequest(
    @Pattern(regexp = "CUSTOMER|VETERINARIAN|RECEPTIONIST|ADMIN")
    String roleName,
    UserStatus statusCode
) {}
