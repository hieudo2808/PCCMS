package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityHelper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    private SecurityHelper securityHelper;

    @Mock
    private PetMapper petMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PetServiceImpl petService;

    @Captor
    private ArgumentCaptor<PetDeactivatedEvent> eventCaptor;

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
            given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
        }

        if ("CREATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
            given(userRepository.findById(currentUserId)).willReturn(Optional.of(owner));
            given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
            given(petMapper.toEntity(createRequest)).willReturn(new Pets());
            given(petRepository.save(any(Pets.class))).willReturn(mockPet);
            given(petMapper.toResponse(mockPet)).willReturn(
                    new com.astral.express.pccms.pet.dto.response.PetResponse(
                            petId, currentUserId, "Milo", speciesId, "Chó", null, null,
                            PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown",
                            null, null, null, null, true));
        }

        if ("UPDATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
            given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));
            given(petMapper.toResponse(any(Pets.class))).willReturn(
                    new com.astral.express.pccms.pet.dto.response.PetResponse(
                            petId, owner.getId(), "Milo Updated", speciesId, "Chó", null, null,
                            PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown",
                            null, null, null, null, true));
        }

        if ("DEACTIVATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));
        }

        if ("GET_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(petMapper.toResponse(mockPet)).willReturn(
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
                    verify(petMapper).toResponse(mockPet);
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
}
