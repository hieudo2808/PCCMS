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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                .filter(s -> !isCorruptedCatalogName(s.getName()))
                .map(s -> new PetSpeciesResponse(s.getId(), s.getName()))
                .toList();
    }

    private static boolean isCorruptedCatalogName(String name) {
        return name == null || name.contains("?") || name.contains("\uFFFD");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetBreedResponse> listBreedsBySpecies(UUID speciesId) {
        petSpeciesRepository.findByIdAndIsActiveTrue(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_SPECIES_NOT_FOUND));

        Map<String, PetBreedResponse> uniqueByName = new LinkedHashMap<>();
        petBreedsRepository.findBySpeciesIdAndIsActiveTrueOrderByNameAsc(speciesId).stream()
                .filter(b -> !isCorruptedCatalogName(b.getName()))
                .forEach(b -> uniqueByName.putIfAbsent(
                        b.getName(),
                        new PetBreedResponse(b.getId(), speciesId, b.getName())));
        return List.copyOf(uniqueByName.values());
    }
}
