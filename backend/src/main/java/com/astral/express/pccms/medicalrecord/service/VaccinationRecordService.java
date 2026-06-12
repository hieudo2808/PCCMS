package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicalrecord.dto.request.CreateVaccinationRequest;
import com.astral.express.pccms.medicalrecord.entity.VaccinationRecord;
import com.astral.express.pccms.medicalrecord.repository.VaccinationRecordRepository;
import com.astral.express.pccms.medicalrecord.service.VaccinationRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VaccinationRecordService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
@Transactional
    public void createVaccinationRecord(CreateVaccinationRequest request) {
        if (request.nextDueDate() != null && request.nextDueDate().isBefore(request.vaccinationDate())) {
            throw new BusinessException(ErrorCode.ERR_VACC_001_INVALID_DUE_DATE);
        }

        VaccinationRecord record = new VaccinationRecord();
        record.setPetId(request.petId());
        record.setMedicalRecordId(request.medicalRecordId());
        record.setVaccineName(request.vaccineName());
        record.setVaccinationDate(request.vaccinationDate());
        record.setNextDueDate(request.nextDueDate());
        record.setNote(request.note());
        record.setCreatedBy(request.createdBy());

        vaccinationRecordRepository.save(record);
    }
}


