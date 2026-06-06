package com.astral.express.pccms.appointment.dto.response;

import java.util.List;
import java.util.UUID;

public record CustomerLookupResponse(
        UUID ownerId,
        String ownerName,
        String phone,
        List<PetSummary> pets
) {
    public record PetSummary(UUID id, String name) {}
}
