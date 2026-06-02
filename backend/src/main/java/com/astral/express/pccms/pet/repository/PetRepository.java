package com.astral.express.pccms.pet.repository;

import com.astral.express.pccms.pet.entity.Pets;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pets, UUID> {
}
