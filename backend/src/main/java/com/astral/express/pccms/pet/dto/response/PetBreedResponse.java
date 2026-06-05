package com.astral.express.pccms.pet.dto.response;

import java.util.UUID;

public record PetBreedResponse(UUID id, UUID speciesId, String name) {}
