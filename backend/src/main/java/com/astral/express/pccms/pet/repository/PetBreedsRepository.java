package com.astral.express.pccms.pet.repository;

import com.astral.express.pccms.pet.entity.PetBreeds;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetBreedsRepository extends JpaRepository<PetBreeds, UUID> {

    Optional<PetBreeds> findByIdAndIsActiveTrue(UUID id);

    List<PetBreeds> findBySpeciesIdAndIsActiveTrueOrderByNameAsc(UUID speciesId);
}
