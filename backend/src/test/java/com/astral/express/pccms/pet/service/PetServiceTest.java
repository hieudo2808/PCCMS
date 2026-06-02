package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.entity.PetSex;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.mapper.PetMapper;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.identity.security.SecurityHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.astral.express.pccms.medicalrecord.service.HealthAlertService;
import com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse;
import org.springframework.context.ApplicationEventPublisher;
import com.astral.express.pccms.pet.event.PetDeactivatedEvent;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

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
    private UserRepository userRepository;
    
    @Mock
    private SecurityHelper securityHelper;
    
    @Mock
    private PetMapper petMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HealthAlertService healthAlertService;

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
        // GIVEN
        LocalDate birthDate = birthDateStr != null && !birthDateStr.trim().isEmpty() 
            ? LocalDate.parse(birthDateStr) : null;
        
        CreatePetRequest createRequest = new CreatePetRequest(
                "Milo", UUID.randomUUID(), null, PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown", null, null, null, null
        );
        com.astral.express.pccms.pet.dto.request.UpdatePetRequest updateRequest = new com.astral.express.pccms.pet.dto.request.UpdatePetRequest(
                "Milo Updated", UUID.randomUUID(), null, PetSex.MALE, birthDate, estimatedAgeMonths, weightKg, "Brown", null, null, null, null
        );
        
        UUID currentUserId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        Users owner = new Users();
        if ("IDOR_FORBIDDEN".equals(mockState)) {
            owner.setUserId(UUID.randomUUID()); // Khác với currentUserId
        } else {
            owner.setUserId(currentUserId);
        }
        
        Pets mockPet = new Pets();
        mockPet.setId(petId);
        mockPet.setOwner(owner);
        mockPet.setIsActive(true);

        if (!"CREATE_PET".equals(action)) {
            given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
            given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
        }

        if ("CREATE_PET".equals(action) && "SUCCESS".equals(expectedResult)) {
            given(securityHelper.getCurrentUserId()).willReturn(currentUserId);
            given(userRepository.findById(currentUserId)).willReturn(Optional.of(owner));
            given(petMapper.toEntity(createRequest)).willReturn(new Pets());
            given(petRepository.save(any(Pets.class))).willReturn(new Pets());
        }

        if (("UPDATE_PET".equals(action) || "DEACTIVATE_PET".equals(action)) && "SUCCESS".equals(expectedResult)) {
            given(petRepository.save(any(Pets.class))).willAnswer(inv -> inv.getArgument(0));
        }

        if (("UPDATE_PET".equals(action) || "GET_PET".equals(action)) && "SUCCESS".equals(expectedResult)) {
            given(healthAlertService.getUnresolvedAlertsByPetId(any(UUID.class)))
                    .willReturn(Collections.emptyList());
        }

        // WHEN & THEN
        if ("EXCEPTION".equals(expectedResult)) {
            ErrorCode expectedErrorCode = ErrorCode.valueOf(errorCodeStr);
            assertThatThrownBy(() -> {
                switch (action) {
                    case "CREATE_PET": petService.createPet(createRequest); break;
                    case "UPDATE_PET": petService.updatePet(petId, updateRequest); break;
                    case "GET_PET": petService.getPet(petId); break;
                    case "DEACTIVATE_PET": petService.deactivatePet(petId); break;
                }
            }).isInstanceOf(BusinessException.class)
              .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
        } else {
            switch (action) {
                case "CREATE_PET":
                    petService.createPet(createRequest);
                    break;
                case "UPDATE_PET":
                    petService.updatePet(petId, updateRequest);
                    verify(petMapper).updatePetFromRequest(updateRequest, mockPet);
                    break;
                case "GET_PET":
                    petService.getPet(petId);
                    verify(petMapper).toResponse(mockPet, Collections.emptyList());
                    break;
                case "DEACTIVATE_PET":
                    petService.deactivatePet(petId);
                    assertThat(mockPet.getIsActive()).isFalse();
                    verify(eventPublisher).publishEvent(eventCaptor.capture());
                    PetDeactivatedEvent capturedEvent = eventCaptor.getValue();
                    assertThat(capturedEvent.petId()).isEqualTo(petId);
                    assertThat(capturedEvent.ownerId()).isEqualTo(owner.getUserId());
                    break;
            }
        }
    }
}
