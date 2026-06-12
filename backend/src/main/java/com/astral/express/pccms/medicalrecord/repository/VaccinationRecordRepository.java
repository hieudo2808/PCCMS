package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.entity.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {
    List<VaccinationRecord> findByPetId(UUID petId);
}
