package com.astral.express.pccms.pet.repository;

import com.astral.express.pccms.pet.entity.PetSpecies;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetSpeciesRepository extends JpaRepository<PetSpecies, UUID> {

    Optional<PetSpecies> findByIdAndIsActiveTrue(UUID id);

    List<PetSpecies> findAllByIsActiveTrueOrderByNameAsc();
}
