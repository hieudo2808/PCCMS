package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.filemedia.service.MediaUploadCommand;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.request.CareLogRequest;
import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import com.astral.express.pccms.reception.repository.BoardingCareLogCommandRepository;
import com.astral.express.pccms.reception.repository.BoardingCareLogQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardingCareLogService {
    private final SecurityContextService securityContextService;
    private final FileMediaService fileMediaService;
    private final BoardingCareLogQueryRepository boardingCareLogQueryRepository;
    private final BoardingCareLogCommandRepository boardingCareLogCommandRepository;

    @Transactional(readOnly = true)
    public List<BoardingBookingResponse> listBookings(String keyword, String status) {
        return boardingCareLogQueryRepository.listBookings(keyword, status);
    }

    @Transactional(readOnly = true)
    public List<CareLogResponse> listCareLogs(UUID sessionId, UUID petId) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        return boardingCareLogQueryRepository.listCareLogs(currentUserId, sessionId, petId);
    }

    @Transactional(readOnly = true)
    public CareLogResponse getCareLog(UUID id) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        return boardingCareLogQueryRepository.findCareLog(currentUserId, id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG));
    }

    @Transactional
    public CareLogResponse saveCareLog(CareLogRequest request) {
        LocalDate logDate = date(request.logDate(), LocalDate.now());
        ReceptionValidation.validateCareLog(logDate, request.periodCode(), request.feedingStatus(), request.hygieneStatus());
        UUID sessionId = request.sessionId();
        UUID petId = request.petId();
        if (sessionId == null && petId == null) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        if (sessionId == null) {
            sessionId = boardingCareLogCommandRepository.findLatestActiveSessionIdByPet(petId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_004_ACTIVE_BOARDING_SESSION_NOT_FOUND));
        }
        if (petId == null) {
            petId = boardingCareLogCommandRepository.findPetIdBySession(sessionId);
        }

        UUID existingId = boardingCareLogCommandRepository.findExistingCareLogId(sessionId, logDate, request.periodCode())
                .orElse(null);
        if (existingId != null) {
            return updateCareLog(existingId, request);
        }

        UUID staffId = securityContextService.getCurrentUserId();
        UUID workScheduleId = resolveActiveWorkSchedule(staffId, logDate);
        UUID careLogId = boardingCareLogCommandRepository.createCareLog(
                sessionId,
                petId,
                staffId,
                workScheduleId,
                logDate,
                request.periodCode(),
                request.feedingStatus(),
                request.hygieneStatus(),
                request.healthNote(),
                request.staffNote());
        return getCareLog(careLogId);
    }

    @Transactional
    public CareLogResponse updateCareLog(UUID id, CareLogRequest request) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        assertCareLogEditable(id, currentUserId);
        LocalDate logDate = date(request.logDate(), LocalDate.now());
        ReceptionValidation.validateCareLog(logDate, request.periodCode(), request.feedingStatus(), request.hygieneStatus());
        boardingCareLogCommandRepository.updateCareLog(
                id,
                logDate,
                request.periodCode(),
                request.feedingStatus(),
                request.hygieneStatus(),
                request.healthNote(),
                request.staffNote());
        return getCareLog(id);
    }

    @Transactional
    public void deleteCareLog(UUID id) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        assertCareLogEditable(id, currentUserId);
        boardingCareLogCommandRepository.softDeleteCareLog(id, currentUserId);
    }

    @Transactional
    public CareLogMediaResponse uploadMedia(UUID careLogId, MediaUploadCommand media) {
        if (media == null || media.bytes() == null || media.bytes().length == 0) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        UUID currentUserId = securityContextService.getCurrentUserId();
        assertCareLogEditable(careLogId, currentUserId);
        ReceptionValidation.validateCareLogMedia(media.sizeBytes(), media.contentType());
        UploadedFileResponse uploaded = fileMediaService.uploadOwnerVisibleMedia(media, currentUserId);
        boardingCareLogCommandRepository.insertCareLogMedia(careLogId, uploaded.id(), "Anh/video nhat ky luu tru");
        return new CareLogMediaResponse(
                uploaded.id(),
                media.originalFilename() == null ? "care-log-image" : media.originalFilename(),
                uploaded.url(),
                uploaded.mimeType(),
                uploaded.sizeBytes(),
                currentUserId,
                "OWNER_VISIBLE"
        );
    }

    private UUID resolveActiveWorkSchedule(UUID staffId, LocalDate logDate) {
        return boardingCareLogCommandRepository.findActiveWorkScheduleId(staffId, logDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_007_CARE_LOG_LOCKED));
    }

    private void assertCareLogEditable(UUID careLogId, UUID currentUserId) {
        Boolean canEdit = boardingCareLogCommandRepository.canEditCareLog(careLogId, currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG));
        if (!Boolean.TRUE.equals(canEdit)) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_007_CARE_LOG_LOCKED);
        }
    }

    private LocalDate date(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }
}
