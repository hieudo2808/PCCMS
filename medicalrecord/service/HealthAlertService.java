package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.request.CreateHealthAlertRequest;

import java.util.List;
import java.util.UUID;

public interface HealthAlertService {
    void createHealthAlert(CreateHealthAlertRequest request);
    void resolveHealthAlert(UUID alertId, UUID resolvedBy);
    List<com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse> getUnresolvedAlertsByPetId(UUID petId);
}
