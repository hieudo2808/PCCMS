package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PetService {

    PetResponse createPet(CreatePetRequest request);

    PetResponse updatePet(UUID petId, UpdatePetRequest request);

    PetResponse getPet(UUID petId);

    PageResponse<PetResponse> listPets(UUID ownerId, Boolean isActive, Pageable pageable);

    void deactivatePet(UUID petId);
}
