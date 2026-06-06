package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PetService {
    PetResponse createPet(CreatePetRequest request);
    PageResponse<PetResponse> listPets(Boolean isActive, Pageable pageable);
    PetResponse updatePet(java.util.UUID petId, com.astral.express.pccms.pet.dto.request.UpdatePetRequest request);
    PetResponse getPet(java.util.UUID petId);
    void deactivatePet(java.util.UUID petId);
}
