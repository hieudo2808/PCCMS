package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.dto.response.PrescriptionItemResponse;
import com.astral.express.pccms.medicalrecord.dto.response.PrescriptionResponse;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicineRepository medicineRepository;
    private final SecurityContextService SecurityContextService;
@Transactional
    public PrescriptionResponse createPrescription(UUID medicalRecordId, CreatePrescriptionRequest request) {
        // Validate medical record
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_400_BAD_REQUEST));

        Prescription prescription = new Prescription();
        prescription.setPrescriptionCode("PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        prescription.setMedicalRecordId(medicalRecordId);
        prescription.setVetId(resolveVetId(request, record));
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

        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved);
    }
public List<PrescriptionResponse> listPrescriptions(UUID medicalRecordId) {
        if (!medicalRecordRepository.existsById(medicalRecordId)) {
            throw new BusinessException(ErrorCode.ERR_400_BAD_REQUEST);
        }

        return prescriptionRepository.findByMedicalRecordIdOrderByIssuedAtDesc(medicalRecordId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UUID resolveVetId(CreatePrescriptionRequest request, MedicalRecord record) {
        UUID currentUserId = SecurityContextService == null ? null : SecurityContextService.getCurrentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        if (request.vetId() != null) {
            return request.vetId();
        }
        if (record.getVetId() != null) {
            return record.getVetId();
        }
        throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
    }

    private PrescriptionResponse toResponse(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPrescriptionCode(),
                prescription.getMedicalRecordId(),
                prescription.getVetId(),
                prescription.getNote(),
                prescription.getIssuedAt(),
                prescription.getItems().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private PrescriptionItemResponse toItemResponse(PrescriptionItem item) {
        Medicine medicine = medicineRepository.findById(item.getMedicineId()).orElse(null);
        return new PrescriptionItemResponse(
                item.getId(),
                item.getMedicineId(),
                medicine == null ? null : medicine.getName(),
                medicine == null ? null : medicine.getUnit(),
                item.getDosage(),
                item.getQuantity(),
                item.getInstruction(),
                item.getUnitPriceVnd()
        );
    }
}


