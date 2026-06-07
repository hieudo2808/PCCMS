package com.astral.express.pccms.medicalrecord.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.Prescription;
import com.astral.express.pccms.medicalrecord.entity.PrescriptionItem;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicalrecord.repository.PrescriptionRepository;
import com.astral.express.pccms.medicalrecord.service.PrescriptionService;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
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
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicineRepository medicineRepository;

    @Override
    @Transactional
    public void createPrescription(UUID medicalRecordId, CreatePrescriptionRequest request) {
        // Validate medical record
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_400_BAD_REQUEST));
        
        if (record.getRecordStatus() == RecordStatus.FINALIZED) {
            throw new BusinessException(ErrorCode.ERR_MED_001_RECORD_LOCKED);
        }

        Prescription prescription = new Prescription();
        prescription.setPrescriptionCode("PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        prescription.setMedicalRecordId(medicalRecordId);
        prescription.setVetId(request.vetId());
        prescription.setNote(request.note());
        prescription.setIssuedAt(OffsetDateTime.now());

        // Process items and lock inventory
        for (PrescriptionItemRequest itemRequest : request.items()) {
            // Lock medicine
            Medicine medicine = medicineRepository.findByIdWithLock(itemRequest.medicineId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));
            
            // Check stock
            if (medicine.getCurrentStock() < itemRequest.quantity()) {
                throw new BusinessException(ErrorCode.ERR_MED_002_INSUFFICIENT_STOCK);
            }

            // Deduct stock
            medicine.setCurrentStock(medicine.getCurrentStock() - itemRequest.quantity());
            medicineRepository.save(medicine);

            // Add item
            PrescriptionItem prescriptionItem = new PrescriptionItem();
            prescriptionItem.setMedicineId(medicine.getId());
            prescriptionItem.setDosage(itemRequest.dosage() != null ? itemRequest.dosage() : "");
            prescriptionItem.setQuantity(itemRequest.quantity());
            prescriptionItem.setInstruction(itemRequest.instruction());
            prescriptionItem.setUnitPriceVnd(medicine.getUnitPriceVnd());

            prescription.addItem(prescriptionItem);
        }

        prescriptionRepository.save(prescription);
    }
}
