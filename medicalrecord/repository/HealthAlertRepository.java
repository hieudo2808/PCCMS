package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.entity.HealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthAlertRepository extends JpaRepository<HealthAlert, UUID> {
    List<HealthAlert> findByPetIdAndResolvedAtIsNull(UUID petId);
}
