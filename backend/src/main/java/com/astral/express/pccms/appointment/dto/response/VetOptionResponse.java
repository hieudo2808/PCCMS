package com.astral.express.pccms.appointment.dto.response;

import java.util.UUID;

public record VetOptionResponse(
        UUID id,
        String fullName,
        boolean available
) {}
