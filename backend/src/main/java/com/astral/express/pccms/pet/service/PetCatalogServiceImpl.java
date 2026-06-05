package com.astral.express.pccms.pet.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.dto.response.PetBreedResponse;
import com.astral.express.pccms.pet.dto.response.PetSpeciesResponse;
import com.astral.express.pccms.pet.repository.PetBreedsRepository;
import com.astral.express.pccms.pet.repository.PetSpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetCatalogServiceImpl implements PetCatalogService {

    private final PetSpeciesRepository petSpeciesRepository;
    private final PetBreedsRepository petBreedsRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PetSpeciesResponse> listActiveSpecies() {
        return petSpeciesRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(s -> new PetSpeciesResponse(s.getId(), s.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetBreedResponse> listBreedsBySpecies(UUID speciesId) {
        petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_SPECIES_NOT_FOUND));

        return petBreedsRepository.findBySpeciesIdAndIsActiveTrueOrderByNameAsc(speciesId).stream()
                .map(b -> new PetBreedResponse(b.getId(), speciesId, b.getName()))
                .toList();
    }
}
