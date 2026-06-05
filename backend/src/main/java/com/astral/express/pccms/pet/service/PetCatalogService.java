package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.pet.dto.response.PetBreedResponse;
import com.astral.express.pccms.pet.dto.response.PetSpeciesResponse;

import java.util.List;
import java.util.UUID;

public interface PetCatalogService {

    List<PetSpeciesResponse> listActiveSpecies();

    List<PetBreedResponse> listBreedsBySpecies(UUID speciesId);
}
