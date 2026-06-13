package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.entity.PetSex;
import com.astral.express.pccms.pet.entity.PetSpecies;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.event.PetDeactivatedEvent;
import com.astral.express.pccms.pet.mapper.PetMapper;
import com.astral.express.pccms.pet.repository.PetBreedsRepository;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.pet.repository.PetSpeciesRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetSpeciesRepository petSpeciesRepository;

    @Mock
    private PetBreedsRepository petBreedsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextService SecurityContextService;

    @Mock
    private PetMapper petMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PetServiceImpl petService;

    @Captor
    private ArgumentCaptor<PetDeactivatedEvent> eventCaptor;

    @Test
    void should_ListCurrentOwnerPetsWithoutStatusFilter_whenIsActiveIsOmitted() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        Users owner = new Users();
        owner.setId(ownerId);

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setOwner(owner);
        pet.setSpecies(species);
        pet.setIsActive(true);

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(petRepository.findByOwner_Id(ownerId, pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        petId, ownerId, "Milo", speciesId, "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets((UUID) null, null, pageable);

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().get(0).id()).isEqualTo(petId);
        verify(petRepository).findByOwner_Id(ownerId, pageable);
    }

    @Test
    void should_ListCurrentOwnerPets_WithIsActive() {
        UUID ownerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Users owner = new Users();
        owner.setId(ownerId);
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setOwner(owner);

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(SecurityContextService.isAdminOrStaff()).willReturn(false);
        given(petRepository.findByOwner_IdAndIsActive(ownerId, true, pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        pet.getId(), ownerId, "Milo", UUID.randomUUID(), "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets(true, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_ListAllPets_AsAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());

        given(SecurityContextService.isAdminOrStaff()).willReturn(true);
        given(petRepository.findByIsActive(true, pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        pet.getId(), UUID.randomUUID(), "Milo", UUID.randomUUID(), "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets(true, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_ListAllPetsWithoutStatusFilter_AsAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());

        given(SecurityContextService.isAdminOrStaff()).willReturn(true);
        given(petRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        pet.getId(), UUID.randomUUID(), "Milo", UUID.randomUUID(), "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets((Boolean) null, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_ListPetsByOwnerId_AsAdmin() {
        UUID ownerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());

        given(SecurityContextService.isAdminOrStaff()).willReturn(true);
        given(petRepository.findByOwner_IdAndIsActive(ownerId, true, pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        pet.getId(), UUID.randomUUID(), "Milo", UUID.randomUUID(), "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets(ownerId, true, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_ListPetsByOwnerIdWithoutStatusFilter_AsAdmin() {
        UUID ownerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());

        given(SecurityContextService.isAdminOrStaff()).willReturn(true);
        given(petRepository.findByOwner_Id(ownerId, pageable))
                .willReturn(new PageImpl<>(List.of(pet), pageable, 1));
        given(petMapper.toResponse(eq(pet), anyList())).willReturn(
                new com.astral.express.pccms.pet.dto.response.PetResponse(
                        pet.getId(), UUID.randomUUID(), "Milo", UUID.randomUUID(), "Chó", null, null,
                        PetSex.MALE, null, 12, BigDecimal.valueOf(5), "Brown",
                        null, null, null, null, true));

        var response = petService.listPets(ownerId, null, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_ListPetsByOwnerId_ThrowForbidden() {
        UUID ownerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        given(SecurityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(SecurityContextService.isAdminOrStaff()).willReturn(false);

        assertThatThrownBy(() -> petService.listPets(ownerId, true, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @ParameterizedTest(name = "[{1}] {2}: {9}")
    @CsvFileSource(resources = "/testcases/pet-age-validation.csv", numLinesToSkip = 1)
    void executePetServiceTests(
            String ruleId,
            String caseId,
            String action,
            String mockState,
            String birthDateStr,
            Integer estimatedAgeMonths,
            BigDecimal weightKg,
            String expectedResult,
            String errorCodeStr,
            String note
    ) {
        LocalDate birthDate = birthDateStr != null && !birthDateStr.trim().isEmpty()
                ? LocalDate.parse(birthDateStr) : null;

        UUID speciesId = UUID.randomUUID();
        CreatePetRequest createRequest = new CreatePetRequest(
                null, "Milo", speciesId, null, PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown", null, null, null, null
        );
        com.astral.express.pccms.pet.dto.request.UpdatePetRequest updateRequest = new com.astral.express.pccms.pet.dto.request.UpdatePetRequest(
                "Milo Updated", speciesId, null, PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown", null, null, null, null
        );

        UUID currentUserId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();

        Users owner = new Users();
        if ("IDOR_FORBIDDEN".equals(mockState)) {
            owner.setId(UUID.randomUUID());
        } else {
            owner.setId(currentUserId);
        }

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();

        Pets mockPet = new Pets();
        mockPet.setId(petId);
        mockPet.setOwner(owner);
        mockPet.setSpecies(species);
        mockPet.setIsActive(true);
        mockPet.setBirthDate(birthDate);
        mockPet.setEstimatedAgeMonths(estimatedAgeMonths);
        mockPet.setWeightKg(weightKg);

        if (!"CREATE_PET".equals(action)) {
            given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
            given(SecurityContextService.getCurrentUserId()).willReturn(currentUserId);
        }

        if ("CREATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(SecurityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(userRepository.findById(currentUserId)).willReturn(Optional.of(owner));
            given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
            given(petMapper.toEntity(createRequest)).willReturn(new Pets());
            given(petRepository.save(any(Pets.class))).willReturn(mockPet);
            given(petMapper.toResponse(eq(mockPet), anyList())).willReturn(
                    new com.astral.express.pccms.pet.dto.response.PetResponse(
                            petId, currentUserId, "Milo", speciesId, "Chó", null, null,
                            PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown",
                            null, null, null, null, true));
        }

        if ("UPDATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
            given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toResponse(any(Pets.class), anyList())).willReturn(
                    new com.astral.express.pccms.pet.dto.response.PetResponse(
                            petId, owner.getId(), "Milo Updated", speciesId, "Chó", null, null,
                            PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown",
                            null, null, null, null, true));
        }

        if ("DEACTIVATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));
        }

        if ("GET_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petMapper.toResponse(eq(mockPet), anyList())).willReturn(
                    new com.astral.express.pccms.pet.dto.response.PetResponse(
                            petId, owner.getId(), "Milo", speciesId, "Chó", null, null,
                            PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown",
                            null, null, null, null, true));
        }

        if ("EXCEPTION".equals(expectedResult)) {
            ErrorCode expectedErrorCode = ErrorCode.valueOf(errorCodeStr);
            assertThatThrownBy(() -> {
                switch (action) {
                    case "CREATE_PET" -> petService.createPet(createRequest);
                    case "UPDATE_PET" -> petService.updatePet(petId, updateRequest);
                    case "GET_PET" -> petService.getPet(petId);
                    case "DEACTIVATE_PET" -> petService.deactivatePet(petId);
                    default -> throw new IllegalStateException("Unknown action: " + action);
                }
            }).isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
        } else {
            switch (action) {
                case "CREATE_PET" -> petService.createPet(createRequest);
                case "UPDATE_PET" -> {
                    petService.updatePet(petId, updateRequest);
                    verify(petMapper).updatePetFromRequest(updateRequest, mockPet);
                }
                case "GET_PET" -> {
                    petService.getPet(petId);
                    verify(petMapper).toResponse(eq(mockPet), anyList());
                }
                case "DEACTIVATE_PET" -> {
                    petService.deactivatePet(petId);
                    assertThat(mockPet.getIsActive()).isFalse();
                    verify(eventPublisher).publishEvent(eventCaptor.capture());
                    PetDeactivatedEvent capturedEvent = eventCaptor.getValue();
                    assertThat(capturedEvent.petId()).isEqualTo(petId);
                    assertThat(capturedEvent.ownerId()).isEqualTo(owner.getId());
                }
                default -> throw new IllegalStateException("Unknown action: " + action);
            }
        }
    }

    @Test
    void should_updatePet_WithBreed() {
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UUID breedId = UUID.randomUUID();
        com.astral.express.pccms.pet.dto.request.UpdatePetRequest updateRequest = new com.astral.express.pccms.pet.dto.request.UpdatePetRequest(
                "Milo Updated", speciesId, breedId, PetSex.MALE, LocalDate.now(), 12, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );

        Users owner = new Users();
        owner.setId(UUID.randomUUID());

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();
        com.astral.express.pccms.pet.entity.PetBreeds breed = new com.astral.express.pccms.pet.entity.PetBreeds();
        breed.setId(breedId);
        breed.setSpecies(species);

        Pets mockPet = new Pets();
        mockPet.setId(petId);
        mockPet.setOwner(owner);
        mockPet.setSpecies(species);
        mockPet.setBirthDate(LocalDate.now());
        mockPet.setEstimatedAgeMonths(12);
        mockPet.setWeightKg(BigDecimal.valueOf(5));

        given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
        given(SecurityContextService.getCurrentUserId()).willReturn(owner.getId());
        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petBreedsRepository.findByIdAndIsActiveTrue(breedId)).willReturn(Optional.of(breed));
        given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));

        petService.updatePet(petId, updateRequest);

        verify(petRepository).save(any(Pets.class));
    }

    @Test
    void should_createPet_WithBreed() {
        UUID ownerId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UUID breedId = UUID.randomUUID();
        CreatePetRequest createRequest = new CreatePetRequest(
                ownerId, "Milo", speciesId, breedId, PetSex.MALE, LocalDate.now(), 12, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );

        Users owner = new Users();
        owner.setId(ownerId);

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();
        com.astral.express.pccms.pet.entity.PetBreeds breed = new com.astral.express.pccms.pet.entity.PetBreeds();
        breed.setId(breedId);
        breed.setSpecies(species);

        given(SecurityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(SecurityContextService.isAdminOrStaff()).willReturn(true);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petBreedsRepository.findByIdAndIsActiveTrue(breedId)).willReturn(Optional.of(breed));
        given(petMapper.toEntity(createRequest)).willReturn(new Pets());
        given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));

        petService.createPet(createRequest);

        verify(petRepository).save(any(Pets.class));
    }

    @Test
    void should_createPet_ThrowBreedSpeciesMismatch() {
        UUID ownerId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UUID breedId = UUID.randomUUID();
        CreatePetRequest createRequest = new CreatePetRequest(
                ownerId, "Milo", speciesId, breedId, PetSex.MALE, LocalDate.now(), 12, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );

        Users owner = new Users();
        owner.setId(ownerId);

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();
        PetSpecies wrongSpecies = PetSpecies.builder().id(UUID.randomUUID()).name("Mèo").build();
        com.astral.express.pccms.pet.entity.PetBreeds breed = new com.astral.express.pccms.pet.entity.PetBreeds();
        breed.setId(breedId);
        breed.setSpecies(wrongSpecies);

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petBreedsRepository.findByIdAndIsActiveTrue(breedId)).willReturn(Optional.of(breed));

        assertThatThrownBy(() -> petService.createPet(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_BREED_SPECIES_MISMATCH);
    }
    
    @Test
    void should_createPet_ThrowInvalidAgeData() {
        CreatePetRequest createRequest = new CreatePetRequest(
                UUID.randomUUID(), "Milo", UUID.randomUUID(), null, PetSex.MALE, null, null, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );
        assertThatThrownBy(() -> petService.createPet(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_INVALID_AGE_DATA);
    }

    @Test
    void should_createPet_ThrowInvalidAgeDataNegative() {
        CreatePetRequest createRequest = new CreatePetRequest(
                UUID.randomUUID(), "Milo", UUID.randomUUID(), null, PetSex.MALE, LocalDate.now(), -1, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );
        assertThatThrownBy(() -> petService.createPet(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_INVALID_AGE_DATA);
    }

    @Test
    void should_createPet_ThrowInvalidWeight() {
        CreatePetRequest createRequest = new CreatePetRequest(
                UUID.randomUUID(), "Milo", UUID.randomUUID(), null, PetSex.MALE, LocalDate.now(), 1, BigDecimal.ZERO, "Brown", null, null, null, null
        );
        assertThatThrownBy(() -> petService.createPet(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_INVALID_WEIGHT);
    }
    
    @Test
    void should_createPet_ThrowBreedNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UUID breedId = UUID.randomUUID();
        CreatePetRequest createRequest = new CreatePetRequest(
                ownerId, "Milo", speciesId, breedId, PetSex.MALE, LocalDate.now(), 12, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );

        Users owner = new Users();
        owner.setId(ownerId);

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petBreedsRepository.findByIdAndIsActiveTrue(breedId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> petService.createPet(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_BREED_NOT_FOUND);
    }

    @Test
    void should_updatePet_RemoveBreed() {
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        com.astral.express.pccms.pet.dto.request.UpdatePetRequest updateRequest = new com.astral.express.pccms.pet.dto.request.UpdatePetRequest(
                "Milo Updated", speciesId, null, PetSex.MALE, LocalDate.now(), 12, BigDecimal.valueOf(5), "Brown", null, null, null, null
        );

        Users owner = new Users();
        owner.setId(UUID.randomUUID());

        PetSpecies species = PetSpecies.builder().id(speciesId).name("Chó").build();
        com.astral.express.pccms.pet.entity.PetBreeds breed = new com.astral.express.pccms.pet.entity.PetBreeds();
        breed.setId(UUID.randomUUID());
        breed.setSpecies(species);

        Pets mockPet = new Pets();
        mockPet.setId(petId);
        mockPet.setOwner(owner);
        mockPet.setSpecies(species);
        mockPet.setBreed(breed);
        mockPet.setBirthDate(LocalDate.now());
        mockPet.setEstimatedAgeMonths(12);
        mockPet.setWeightKg(BigDecimal.valueOf(5));

        given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
        given(SecurityContextService.getCurrentUserId()).willReturn(owner.getId());
        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));

        petService.updatePet(petId, updateRequest);

        assertThat(mockPet.getBreed()).isNull();
        verify(petRepository).save(any(Pets.class));
    }

    @Test
    void should_deactivatePet_AlreadyDeactivated() {
        UUID petId = UUID.randomUUID();
        Users owner = new Users();
        owner.setId(UUID.randomUUID());

        Pets mockPet = new Pets();
        mockPet.setId(petId);
        mockPet.setOwner(owner);
        mockPet.setIsActive(false);

        given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
        given(SecurityContextService.getCurrentUserId()).willReturn(owner.getId());

        petService.deactivatePet(petId);

        verify(petRepository, org.mockito.Mockito.never()).save(any());
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }
}
