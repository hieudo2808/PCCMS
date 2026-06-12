package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.pet.dto.response.PetBreedResponse;
import com.astral.express.pccms.pet.dto.response.PetSpeciesResponse;
import com.astral.express.pccms.pet.service.PetCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/pet-catalog")
@RequiredArgsConstructor
public class PetCatalogController {

    private final PetCatalogService petCatalogService;

    @GetMapping("/species")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PetSpeciesResponse>> listSpecies() {
        return ApiResponse.success(petCatalogService.listActiveSpecies());
    }

    @GetMapping("/species/{speciesId}/breeds")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PetBreedResponse>> listBreeds(@PathVariable UUID speciesId) {
        return ApiResponse.success(petCatalogService.listBreedsBySpecies(speciesId));
    }
}
