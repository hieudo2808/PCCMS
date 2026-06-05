package com.astral.express.pccms.pet.repository;

import com.astral.express.pccms.pet.entity.Pets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pets, UUID> {

    @Query("""
            SELECT p FROM Pets p
            WHERE p.owner.id = :ownerId
              AND (:isActive IS NULL OR p.isActive = :isActive)
            """)
    Page<Pets> findByOwnerIdAndIsActive(
            @Param("ownerId") UUID ownerId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
