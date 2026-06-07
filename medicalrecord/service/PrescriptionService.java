package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;

import java.util.UUID;

public interface PrescriptionService {
    void createPrescription(UUID medicalRecordId, CreatePrescriptionRequest request);
}
