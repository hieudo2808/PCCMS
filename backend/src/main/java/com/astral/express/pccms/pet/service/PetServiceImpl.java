package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.mapper.PetMapper;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.entity.PetBreeds;
import com.astral.express.pccms.pet.entity.PetSpecies;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.event.PetDeactivatedEvent;
import com.astral.express.pccms.pet.repository.PetBreedsRepository;
import com.astral.express.pccms.pet.repository.PetSpeciesRepository;
import com.astral.express.pccms.user.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final PetSpeciesRepository petSpeciesRepository;
    private final PetBreedsRepository petBreedsRepository;
    private final UserRepository userRepository;
    private final SecurityContextService SecurityContextService;
    private final PetMapper petMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PetResponse createPet(CreatePetRequest request) {
        validateAgeData(request.birthDate(), request.estimatedAgeMonths());
        validateWeight(request.weightKg());

        UUID ownerId = resolveOwnerIdForCreate(request.ownerId());
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        PetSpecies species = resolveSpecies(request.speciesId());
        PetBreeds breed = resolveBreed(request.breedId(), species);

        Pets pet = petMapper.toEntity(request);
        pet.setOwner(owner);
        pet.setSpecies(species);
        pet.setBreed(breed);

        Pets savedPet = petRepository.save(pet);
        log.info("Created pet with id: {} for owner: {}", savedPet.getId(), ownerId);

        return petMapper.toResponse(savedPet, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PetResponse> listPets(Boolean isActive, Pageable pageable) {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        Page<Pets> pets;
        if (SecurityContextService.isAdminOrStaff()) {
            pets = isActive == null
                    ? petRepository.findAll(pageable)
                    : petRepository.findByIsActive(isActive, pageable);
        } else {
            pets = isActive == null
                    ? petRepository.findByOwner_Id(currentUserId, pageable)
                    : petRepository.findByOwner_IdAndIsActive(currentUserId, isActive, pageable);
        }

        return PageResponse.of(pets.map(pet -> petMapper.toResponse(pet, Collections.emptyList())));
    }

    @Override
    @Transactional
    public PetResponse updatePet(UUID petId, UpdatePetRequest request) {
        Pets pet = findPetOrThrow(petId);
        assertCanAccessPet(pet);

        petMapper.updatePetFromRequest(request, pet);

        if (request.speciesId() != null) {
            pet.setSpecies(resolveSpecies(request.speciesId()));
        }
        if (request.breedId() != null) {
            pet.setBreed(resolveBreed(request.breedId(), pet.getSpecies()));
        } else if (request.breedId() == null && request.speciesId() != null) {
            pet.setBreed(null);
        }

        validateAgeData(pet.getBirthDate(), pet.getEstimatedAgeMonths());
        validateWeight(pet.getWeightKg());

        Pets savedPet = petRepository.save(pet);
        log.info("Updated pet with id: {}", savedPet.getId());

        return petMapper.toResponse(savedPet, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPet(UUID petId) {
        Pets pet = findPetOrThrow(petId);
        assertCanAccessPet(pet);
        return petMapper.toResponse(pet, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PetResponse> listPets(UUID ownerId, Boolean isActive, Pageable pageable) {
        UUID resolvedOwnerId = resolveOwnerIdForList(ownerId);
        Page<Pets> pets = isActive == null
                ? petRepository.findByOwner_Id(resolvedOwnerId, pageable)
                : petRepository.findByOwner_IdAndIsActive(resolvedOwnerId, isActive, pageable);
        return PageResponse.of(
                pets.map(pet -> petMapper.toResponse(pet, Collections.emptyList())));
    }

    @Override
    @Transactional
    public void deactivatePet(UUID petId) {
        Pets pet = findPetOrThrow(petId);
        assertCanAccessPet(pet);

        if (Boolean.FALSE.equals(pet.getIsActive())) {
            return;
        }

        pet.setIsActive(false);
        petRepository.save(pet);
        log.info("Deactivated pet with id: {}", pet.getId());

        eventPublisher.publishEvent(new PetDeactivatedEvent(petId, pet.getOwner().getId()));
    }

    private Pets findPetOrThrow(UUID petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
    }

    private void assertCanAccessPet(Pets pet) {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (!pet.getOwner().getId().equals(currentUserId) && !SecurityContextService.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }

    private UUID resolveOwnerIdForCreate(UUID requestedOwnerId) {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (requestedOwnerId == null) {
            return currentUserId;
        }
        if (!requestedOwnerId.equals(currentUserId) && !SecurityContextService.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        return requestedOwnerId;
    }

    private UUID resolveOwnerIdForList(UUID requestedOwnerId) {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (requestedOwnerId == null || requestedOwnerId.equals(currentUserId)) {
            return currentUserId;
        }
        if (!SecurityContextService.isAdminOrStaff()) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        return requestedOwnerId;
    }

    private PetSpecies resolveSpecies(UUID speciesId) {
        return petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_SPECIES_NOT_FOUND));
    }

    private PetBreeds resolveBreed(UUID breedId, PetSpecies species) {
        if (breedId == null) {
            return null;
        }
        PetBreeds breed = petBreedsRepository.findByIdAndIsActiveTrue(breedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_BREED_NOT_FOUND));
        if (!breed.getSpecies().getId().equals(species.getId())) {
            throw new BusinessException(ErrorCode.ERR_PET_BREED_SPECIES_MISMATCH);
        }
        return breed;
    }

    private void validateAgeData(LocalDate birthDate, Integer estimatedAgeMonths) {
        if (birthDate == null && estimatedAgeMonths == null) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
        if (estimatedAgeMonths != null && estimatedAgeMonths < 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA);
        }
    }

    private void validateWeight(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.ERR_PET_INVALID_WEIGHT);
        }
    }
}
