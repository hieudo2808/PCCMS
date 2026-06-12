package com.astral.express.pccms.user.dto.request;

import com.astral.express.pccms.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AdminUpdateUserRequest(
    @Size(max = 150)
    String fullName,

    @Email
    @Size(max = 255)
    String email,

    @Size(max = 30)
    String phone,

    @Pattern(regexp = "OWNER|STAFF|VETERINARIAN|ADMIN")
    String roleCode,

    UserStatus statusCode
) {}
