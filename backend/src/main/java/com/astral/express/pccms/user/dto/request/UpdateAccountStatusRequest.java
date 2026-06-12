package com.astral.express.pccms.user.dto.request;

import com.astral.express.pccms.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull
        UserStatus statusCode
) {
}
