package com.astral.express.pccms.pet.repository;

import com.astral.express.pccms.pet.entity.Pets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pets, UUID> {
    Page<Pets> findByOwner_Id(UUID ownerId, Pageable pageable);

    Page<Pets> findByOwner_IdAndIsActive(UUID ownerId, Boolean isActive, Pageable pageable);

    Page<Pets> findByIsActive(Boolean isActive, Pageable pageable);
}
