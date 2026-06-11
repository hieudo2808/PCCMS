package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetValidationService {

    private final PetRepository petRepository;

    public Pets findPetOwnedBy(UUID petId, UUID ownerId) {
        Pets pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(pet.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND);
        }
        return pet;
    }
}
