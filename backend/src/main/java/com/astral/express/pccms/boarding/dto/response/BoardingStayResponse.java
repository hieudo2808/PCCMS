package com.astral.express.pccms.boarding.dto.response;

import java.util.UUID;

public record BoardingStayResponse(
        UUID petId,
        String petName,
        String speciesName,
        String breedName
) {}
