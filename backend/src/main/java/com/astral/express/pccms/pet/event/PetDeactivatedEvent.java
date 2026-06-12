package com.astral.express.pccms.pet.event;

import java.util.UUID;

public record PetDeactivatedEvent(UUID petId, UUID ownerId) {
}
