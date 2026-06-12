package com.astral.express.pccms.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignAccountRoleRequest(
        @NotBlank
        String roleCode
) {
}
