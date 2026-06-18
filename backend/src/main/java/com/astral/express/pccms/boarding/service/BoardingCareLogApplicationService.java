package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.CareLogCreateRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CareLogMedia;
import com.astral.express.pccms.boarding.mapper.BoardingMapper;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.boarding.repository.CareLogMediaRepository;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.entity.FileLink;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.filemedia.repository.FileLinkRepository;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.filemedia.service.MediaUploadCommand;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardingCareLogApplicationService {

    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final BoardingBookingRepository boardingBookingRepository;
    private final BoardingSessionRepository boardingSessionRepository;
    private final CareLogRepository careLogRepository;
    private final CareLogMediaRepository careLogMediaRepository;
    private final FileMediaService fileMediaService;
    private final FileAssetRepository fileAssetRepository;
    private final FileLinkRepository fileLinkRepository;
    private final BoardingMapper boardingMapper;

    @Transactional
    public CareLogResponse createCareLog(
            UUID sessionId,
            CareLogCreateRequest request,
            List<MediaUploadCommand> images) {
        Users staff = findUser(requireCurrentUserId());
        BoardingSession session = boardingSessionRepository.findWithDetailsById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND));
        if (session.getStatusCode() != BoardingStatus.CHECKED_IN && session.getStatusCode() != BoardingStatus.IN_STAY) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
        }
        if (careLogRepository.existsBySessionIdAndLogDateAndPeriodCodeAndDeletedAtIsNull(
                sessionId,
                request.logDate(),
                request.periodCode())) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_004_CARE_LOG_DUPLICATED);
        }

        CareLog careLog = careLogRepository.save(CareLog.builder()
                .session(session)
                .pet(session.getBooking().getPet())
                .staff(staff)
                .logDate(request.logDate())
                .periodCode(request.periodCode())
                .feedingStatus(request.feedingStatus())
                .hygieneStatus(request.hygieneStatus())
                .healthNote(request.healthNote())
                .staffNote(request.staffNote())
                .build());
        List<CareLogMedia> media = saveCareLogMedia(
                careLog,
                request.caption(),
                images == null ? Collections.emptyList() : images);
        return boardingMapper.toCareLogResponse(careLog, media);
    }

    public List<CareLogResponse> listCareLogs(UUID bookingId) {
        BoardingBooking booking = findBooking(bookingId);
        assertCanAccessBooking(booking);
        return careLogRepository.findBySessionBookingIdAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(bookingId).stream()
                .map(careLog -> boardingMapper.toCareLogResponse(
                        careLog,
                        careLogMediaRepository.findByCareLogId(careLog.getId())))
                .toList();
    }

    private List<CareLogMedia> saveCareLogMedia(
            CareLog careLog,
            String caption,
            List<MediaUploadCommand> images) {
        return images.stream()
                .map(image -> {
                    UploadedFileResponse uploadedFile = fileMediaService.uploadOwnerVisibleImage(image, requireCurrentUserId());
                    FileAsset fileAsset = fileAssetRepository.findById(uploadedFile.id())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE));
                    fileLinkRepository.save(FileLink.builder()
                            .file(fileAsset)
                            .entityType("CARE_LOG")
                            .entityId(careLog.getId())
                            .purpose("CARE_LOG_MEDIA")
                            .build());
                    return careLogMediaRepository.save(CareLogMedia.builder()
                            .careLog(careLog)
                            .file(fileAsset)
                            .caption(caption)
                            .build());
                })
                .toList();
    }

    private BoardingBooking findBooking(UUID bookingId) {
        return boardingBookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_001_BOOKING_NOT_FOUND));
    }

    private Users findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void assertCanAccessBooking(BoardingBooking booking) {
        if (securityContextService.hasAnyRole("ADMIN", "STAFF")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        if (!booking.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }
}
