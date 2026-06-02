package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;

public interface PetService {
    PetResponse createPet(CreatePetRequest request);
    PetResponse updatePet(java.util.UUID petId, com.astral.express.pccms.pet.dto.request.UpdatePetRequest request);
    PetResponse getPet(java.util.UUID petId);
    void deactivatePet(java.util.UUID petId);
}
