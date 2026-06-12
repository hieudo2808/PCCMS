package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.entity.Prescription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    @EntityGraph(attributePaths = "items")
    List<Prescription> findByMedicalRecordIdOrderByIssuedAtDesc(UUID medicalRecordId);

    @EntityGraph(attributePaths = {"items"})
    List<Prescription> findByMedicalRecordIdIn(List<UUID> medicalRecordIds);
}
