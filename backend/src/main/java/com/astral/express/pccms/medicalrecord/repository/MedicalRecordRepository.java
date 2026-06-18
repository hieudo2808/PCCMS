package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    List<MedicalRecord> findByVetIdOrderByCreatedAtDesc(UUID vetId);
    List<MedicalRecord> findAllByOrderByCreatedAtDesc();
    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

    @Query("""
        SELECT new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse(
            m.id, m.recordCode, m.appointmentId, m.petId, COALESCE(p.name, 'Unknown Pet'), m.vetId, COALESCE(u.fullName, 'Unknown Vet'),
            m.recordStatus, m.temperatureC, m.heartRateBpm, m.respiratoryRateBpm, m.weightKg,
            m.bloodPressure, m.spo2Percent, m.mucousMembraneColor, m.capillaryRefillSeconds,
            m.preliminaryDiagnosis, m.finalDiagnosis, m.treatmentNote,
            m.followUpAt, m.lockedAt, m.createdAt, m.updatedAt
        )
        FROM MedicalRecord m
        LEFT JOIN Pets p ON m.petId = p.id
        LEFT JOIN Users u ON m.vetId = u.id
        WHERE m.vetId = :vetId
        ORDER BY m.createdAt DESC
    """)
    List<MedicalRecordResponse> findResponsesByVetId(@Param("vetId") UUID vetId);

    @Query("""
        SELECT new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse(
            m.id, m.recordCode, m.appointmentId, m.petId, COALESCE(p.name, 'Unknown Pet'), m.vetId, COALESCE(u.fullName, 'Unknown Vet'),
            m.recordStatus, m.temperatureC, m.heartRateBpm, m.respiratoryRateBpm, m.weightKg,
            m.bloodPressure, m.spo2Percent, m.mucousMembraneColor, m.capillaryRefillSeconds,
            m.preliminaryDiagnosis, m.finalDiagnosis, m.treatmentNote,
            m.followUpAt, m.lockedAt, m.createdAt, m.updatedAt
        )
        FROM MedicalRecord m
        LEFT JOIN Pets p ON m.petId = p.id
        LEFT JOIN Users u ON m.vetId = u.id
        ORDER BY m.createdAt DESC
    """)
    List<MedicalRecordResponse> findAllResponses();

    @Query("""
        SELECT new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse(
            m.id, m.recordCode, m.appointmentId, m.petId, COALESCE(p.name, 'Unknown Pet'), m.vetId, COALESCE(u.fullName, 'Unknown Vet'),
            m.recordStatus, m.temperatureC, m.heartRateBpm, m.respiratoryRateBpm, m.weightKg,
            m.bloodPressure, m.spo2Percent, m.mucousMembraneColor, m.capillaryRefillSeconds,
            m.preliminaryDiagnosis, m.finalDiagnosis, m.treatmentNote,
            m.followUpAt, m.lockedAt, m.createdAt, m.updatedAt
        )
        FROM MedicalRecord m
        LEFT JOIN Pets p ON m.petId = p.id
        LEFT JOIN Users u ON m.vetId = u.id
        WHERE m.id = :id
    """)
    Optional<MedicalRecordResponse> findResponseById(@Param("id") UUID id);

    @Query("""
        SELECT new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse(
            m.id, m.recordCode, m.appointmentId, m.petId, COALESCE(p.name, 'Unknown Pet'), m.vetId, COALESCE(u.fullName, 'Unknown Vet'),
            m.recordStatus, m.temperatureC, m.heartRateBpm, m.respiratoryRateBpm, m.weightKg,
            m.bloodPressure, m.spo2Percent, m.mucousMembraneColor, m.capillaryRefillSeconds,
            m.preliminaryDiagnosis, m.finalDiagnosis, m.treatmentNote,
            m.followUpAt, m.lockedAt, m.createdAt, m.updatedAt
        )
        FROM MedicalRecord m
        LEFT JOIN Pets p ON m.petId = p.id
        LEFT JOIN Users u ON m.vetId = u.id
        WHERE m.appointmentId = :appointmentId
    """)
    Optional<MedicalRecordResponse> findResponseByAppointmentId(@Param("appointmentId") UUID appointmentId);

    @Query("""
        SELECT new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse(
            m.id, m.recordCode, m.petId, COALESCE(u.fullName, 'Unknown Vet'), m.temperatureC, m.weightKg,
            m.heartRateBpm, m.respiratoryRateBpm, m.bloodPressure, m.spo2Percent, m.mucousMembraneColor, m.capillaryRefillSeconds,
            m.finalDiagnosis, m.treatmentNote, m.followUpAt, m.createdAt, null
        )
        FROM MedicalRecord m
        LEFT JOIN Users u ON m.vetId = u.id
        WHERE m.petId = :petId AND m.recordStatus = :status
        ORDER BY m.createdAt DESC
    """)
    List<MedicalRecordOwnerResponse> findOwnerResponsesByPetIdAndStatus(
        @Param("petId") UUID petId,
        @Param("status") RecordStatus status
    );
}


