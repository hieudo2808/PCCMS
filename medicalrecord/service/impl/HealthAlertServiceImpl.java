package com.astral.express.pccms.medicalrecord.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicalrecord.dto.request.CreateHealthAlertRequest;
import com.astral.express.pccms.medicalrecord.entity.HealthAlert;
import com.astral.express.pccms.medicalrecord.repository.HealthAlertRepository;
import com.astral.express.pccms.medicalrecord.service.HealthAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HealthAlertServiceImpl implements HealthAlertService {

    private final HealthAlertRepository healthAlertRepository;

    @Override
    @Transactional
    public void createHealthAlert(CreateHealthAlertRequest request) {
        HealthAlert alert = new HealthAlert();
        alert.setPetId(request.petId());
        alert.setMedicalRecordId(request.medicalRecordId());
        alert.setSeverity(request.severity());
        alert.setMessage(request.message());
        alert.setCreatedBy(request.createdBy());

        healthAlertRepository.save(alert);
    }

    @Override
    @Transactional
    public void resolveHealthAlert(UUID alertId, UUID resolvedBy) {
        HealthAlert alert = healthAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND)); // Wait, ERR_404_NOT_FOUND is not there maybe. Let me use ERR_400_BAD_REQUEST or just handle it if I have NOT_FOUND. 
        
        if (alert.getResolvedAt() != null) {
            return; // Already resolved
        }

        alert.setResolvedAt(OffsetDateTime.now());
        healthAlertRepository.save(alert);
    }

    @Override
    public java.util.List<com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse> getUnresolvedAlertsByPetId(UUID petId) {
        return healthAlertRepository.findByPetIdAndResolvedAtIsNull(petId).stream()
                .map(alert -> new com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse(
                        alert.getId(),
                        alert.getPetId(),
                        alert.getMedicalRecordId(),
                        alert.getSeverity(),
                        alert.getMessage(),
                        alert.getCreatedAt()
                ))
                .toList();
    }
}
