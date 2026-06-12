package com.astral.express.pccms.boarding.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BoardingConfirmRequest(
        @NotNull UUID roomId
) {
}
