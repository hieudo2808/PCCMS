package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {
    List<LabResult> findByMedicalRecordIdOrderByCreatedAtDesc(UUID medicalRecordId);
}
