package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.pet.dto.response.PetBreedResponse;
import com.astral.express.pccms.pet.dto.response.PetSpeciesResponse;
import com.astral.express.pccms.pet.service.PetCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PetCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PetCatalogService petCatalogService;

    @InjectMocks
    private PetCatalogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listSpecies_success() throws Exception {
        PetSpeciesResponse response = new PetSpeciesResponse(UUID.randomUUID(), "Dog");
        given(petCatalogService.listActiveSpecies()).willReturn(List.of(response));

        mockMvc.perform(get("/v1/pet-catalog/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Dog"));
    }

    @Test
    void listBreeds_success() throws Exception {
        UUID speciesId = UUID.randomUUID();
        PetBreedResponse response = new PetBreedResponse(UUID.randomUUID(), speciesId, "Husky");
        given(petCatalogService.listBreedsBySpecies(any())).willReturn(List.of(response));

        mockMvc.perform(get("/v1/pet-catalog/species/{speciesId}/breeds", speciesId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Husky"));
    }
}
