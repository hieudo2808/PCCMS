package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.request.CreateVaccinationRequest;

import java.util.UUID;

public interface VaccinationRecordService {
    void createVaccinationRecord(CreateVaccinationRequest request);
}
