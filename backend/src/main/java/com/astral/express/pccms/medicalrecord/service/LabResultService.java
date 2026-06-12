package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.request.CreateLabResultRequest;
import com.astral.express.pccms.medicalrecord.dto.response.LabResultResponse;
import com.astral.express.pccms.medicalrecord.entity.LabResult;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.repository.LabResultRepository;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicalrecord.service.LabResultService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabResultService {
    private final LabResultRepository labResultRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final SecurityContextService SecurityContextService;
public List<LabResultResponse> listLabResults(UUID medicalRecordId) {
        MedicalRecord medicalRecord = findMedicalRecord(medicalRecordId);
        assertCanRead(medicalRecord);
        return labResultRepository.findByMedicalRecordIdOrderByCreatedAtDesc(medicalRecordId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
@Transactional
    public LabResultResponse createLabResult(UUID medicalRecordId, CreateLabResultRequest request) {
        MedicalRecord medicalRecord = findMedicalRecord(medicalRecordId);
        Users creator = userRepository.findById(requireCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        FileAsset file = request.fileId() == null ? null : fileAssetRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));

        LabResult labResult = LabResult.builder()
                .medicalRecord(medicalRecord)
                .testName(request.testName().trim())
                .resultText(request.resultText())
                .file(file)
                .createdBy(creator)
                .build();
        return toResponse(labResultRepository.save(labResult));
    }

    private MedicalRecord findMedicalRecord(UUID medicalRecordId) {
        return medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private void assertCanRead(MedicalRecord medicalRecord) {
        if (SecurityContextService.hasAnyRole("ADMIN", "VETERINARIAN")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        Pets pet = petRepository.findById(medicalRecord.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
        if (!pet.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private LabResultResponse toResponse(LabResult labResult) {
        return new LabResultResponse(
                labResult.getId(),
                labResult.getMedicalRecord().getId(),
                labResult.getTestName(),
                labResult.getResultText(),
                labResult.getFile() == null ? null : labResult.getFile().getId(),
                labResult.getCreatedBy() == null ? null : labResult.getCreatedBy().getId(),
                labResult.getCreatedAt(),
                labResult.getUpdatedAt()
        );
    }
}


