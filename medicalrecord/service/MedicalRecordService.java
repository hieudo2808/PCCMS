package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;

import java.util.UUID;

public interface MedicalRecordService {
    MedicalRecordResponse updateMedicalRecord(UUID recordId, UpdateMedicalRecordRequest request);
    MedicalRecordResponse finalizeMedicalRecord(UUID recordId, FinalizeMedicalRecordRequest request);
}
