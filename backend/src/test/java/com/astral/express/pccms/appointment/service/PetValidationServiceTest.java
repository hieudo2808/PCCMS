package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PetValidationServiceTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetValidationService service;

    @Test
    void findPetOwnedBy_shouldReturnPet_whenValid() {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setIsActive(true);
        Users owner = new Users();
        owner.setId(ownerId);
        pet.setOwner(owner);

        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        Pets result = service.findPetOwnedBy(petId, ownerId);

        assertThat(result).isEqualTo(pet);
    }

    @Test
    void findPetOwnedBy_shouldThrowException_whenNotFound() {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(petRepository.findById(petId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findPetOwnedBy(petId, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_001_NOT_FOUND);
    }

    @Test
    void findPetOwnedBy_shouldThrowException_whenNotOwner() {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Pets pet = new Pets();
        pet.setId(petId);
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        pet.setOwner(owner);

        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        assertThatThrownBy(() -> service.findPetOwnedBy(petId, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void findPetOwnedBy_shouldThrowException_whenInactive() {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setIsActive(false);
        Users owner = new Users();
        owner.setId(ownerId);
        pet.setOwner(owner);

        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        assertThatThrownBy(() -> service.findPetOwnedBy(petId, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_001_NOT_FOUND);
    }
}
