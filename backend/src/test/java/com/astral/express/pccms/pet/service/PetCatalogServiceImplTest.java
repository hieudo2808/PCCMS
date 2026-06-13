package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.PetBreeds;
import com.astral.express.pccms.pet.entity.PetSpecies;
import com.astral.express.pccms.pet.repository.PetBreedsRepository;
import com.astral.express.pccms.pet.repository.PetSpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PetCatalogServiceImplTest {

    @Mock
    private PetSpeciesRepository petSpeciesRepository;

    @Mock
    private PetBreedsRepository petBreedsRepository;

    @InjectMocks
    private PetCatalogServiceImpl petCatalogService;

    @Test
    void should_listActiveSpecies() {
        PetSpecies species1 = PetSpecies.builder().id(UUID.randomUUID()).name("Dog").build();
        PetSpecies species2 = PetSpecies.builder().id(UUID.randomUUID()).name("Cat?").build(); // corrupted
        PetSpecies species3 = PetSpecies.builder().id(UUID.randomUUID()).name(null).build(); // corrupted

        given(petSpeciesRepository.findAllByIsActiveTrueOrderByNameAsc()).willReturn(List.of(species1, species2, species3));

        var res = petCatalogService.listActiveSpecies();
        assertThat(res).hasSize(1);
        assertThat(res.get(0).name()).isEqualTo("Dog");
    }

    @Test
    void should_listBreedsBySpecies() {
        UUID speciesId = UUID.randomUUID();
        PetSpecies species = PetSpecies.builder().id(speciesId).name("Dog").build();

        PetBreeds breed1 = new PetBreeds();
        breed1.setId(UUID.randomUUID());
        breed1.setName("Golden");

        PetBreeds breed2 = new PetBreeds();
        breed2.setId(UUID.randomUUID());
        breed2.setName("Pug?"); // corrupted

        PetBreeds breed3 = new PetBreeds();
        breed3.setId(UUID.randomUUID());
        breed3.setName("Golden"); // duplicate name

        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.of(species));
        given(petBreedsRepository.findBySpeciesIdAndIsActiveTrueOrderByNameAsc(speciesId)).willReturn(List.of(breed1, breed2, breed3));

        var res = petCatalogService.listBreedsBySpecies(speciesId);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).name()).isEqualTo("Golden");
    }

    @Test
    void should_listBreedsBySpecies_throwSpeciesNotFound() {
        UUID speciesId = UUID.randomUUID();

        given(petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> petCatalogService.listBreedsBySpecies(speciesId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_SPECIES_NOT_FOUND);
    }
}
