package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.mapper.PetMapper;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.identity.security.SecurityHelper;
import com.astral.express.pccms.pet.event.PetDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;
    private final PetMapper petMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.astral.express.pccms.medicalrecord.service.HealthAlertService healthAlertService;

    @Override
    @Transactional
    public PetResponse createPet(CreatePetRequest request) {
        if (request.birthDate() == null && request.estimatedAgeMonths() == null) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
        if (request.estimatedAgeMonths() != null && request.estimatedAgeMonths() < 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
        if (request.weightKg() != null && request.weightKg().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_WEIGHT);
        }

        java.util.UUID currentUserId = securityHelper.getCurrentUserId();
        Users owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        Pets pet = petMapper.toEntity(request);
        pet.setOwner(owner);

        Pets savedPet = petRepository.save(pet);
        log.info("Created pet with id: {}", savedPet.getId());

        return petMapper.toResponse(savedPet, java.util.Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PetResponse> listPets(Boolean isActive, Pageable pageable) {
        java.util.UUID currentUserId = securityHelper.getCurrentUserId();
        Page<Pets> pets;
        if (securityHelper.isAdminOrStaff()) {
            pets = isActive == null
                    ? petRepository.findAll(pageable)
                    : petRepository.findByIsActive(isActive, pageable);
        } else {
            pets = isActive == null
                    ? petRepository.findByOwner_Id(currentUserId, pageable)
                    : petRepository.findByOwner_IdAndIsActive(currentUserId, isActive, pageable);
        }

        return PageResponse.of(pets.map(pet -> petMapper.toResponse(pet, java.util.Collections.emptyList())));
    }

    @Override
    @Transactional
    public PetResponse updatePet(java.util.UUID petId, com.astral.express.pccms.pet.dto.request.UpdatePetRequest request) {
        Pets pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));

        java.util.UUID currentUserId = securityHelper.getCurrentUserId();
        if (!pet.getOwner().getId().equals(currentUserId) && !securityHelper.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        if (request.birthDate() == null && request.estimatedAgeMonths() == null) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
        if (request.estimatedAgeMonths() != null && request.estimatedAgeMonths() < 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
        if (request.weightKg() != null && request.weightKg().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_WEIGHT);
        }

        petMapper.updatePetFromRequest(request, pet);

        Pets savedPet = petRepository.save(pet);
        log.info("Updated pet with id: {}", savedPet.getId());

        return petMapper.toResponse(savedPet, healthAlertService.getUnresolvedAlertsByPetId(savedPet.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPet(java.util.UUID petId) {
        Pets pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));

        java.util.UUID currentUserId = securityHelper.getCurrentUserId();
        if (!pet.getOwner().getId().equals(currentUserId) && !securityHelper.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        return petMapper.toResponse(pet, healthAlertService.getUnresolvedAlertsByPetId(pet.getId()));
    }

    @Override
    @Transactional
    public void deactivatePet(java.util.UUID petId) {
        Pets pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));

        java.util.UUID currentUserId = securityHelper.getCurrentUserId();
        if (!pet.getOwner().getId().equals(currentUserId) && !securityHelper.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        pet.setIsActive(false);
        petRepository.save(pet);
        log.info("Deactivated pet with id: {}", pet.getId());

        eventPublisher.publishEvent(new PetDeactivatedEvent(petId, pet.getOwner().getId()));
    }
}
