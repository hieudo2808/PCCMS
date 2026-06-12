package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.entity.FileVisibility;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.filemedia.service.CloudMediaStorageService;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileMediaService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_OWNER_VISIBLE_MEDIA_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    private final CloudMediaStorageService cloudMediaStorageService;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
@Transactional
    public UploadedFileResponse uploadOwnerVisibleImage(MultipartFile file, UUID uploadedByUserId) {
        validateImage(file);
        return uploadOwnerVisibleValidated(file, uploadedByUserId);
    }
@Transactional
    public UploadedFileResponse uploadOwnerVisibleMedia(MultipartFile file, UUID uploadedByUserId) {
        validateOwnerVisibleMedia(file);
        return uploadOwnerVisibleValidated(file, uploadedByUserId);
    }

    private UploadedFileResponse uploadOwnerVisibleValidated(MultipartFile file, UUID uploadedByUserId) {
        Users uploadedBy = userRepository.findById(uploadedByUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        CloudMediaStorageService.StoredMedia storedMedia = cloudMediaStorageService.uploadImage(file, "care-logs");

        FileAsset fileAsset = FileAsset.builder()
                .originalName(file.getOriginalFilename() == null ? "care-log-image" : file.getOriginalFilename())
                .storedKey(storedMedia.secureUrl())
                .mimeType(storedMedia.mimeType())
                .sizeBytes(storedMedia.sizeBytes())
                .uploadedBy(uploadedBy)
                .visibilityCode(FileVisibility.OWNER_VISIBLE)
                .build();
        FileAsset saved = fileAssetRepository.save(fileAsset);
        return new UploadedFileResponse(saved.getId(), saved.getStoredKey(), storedMedia.publicId(), saved.getMimeType(), saved.getSizeBytes());
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
    }

    private void validateOwnerVisibleMedia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
        if (file.getSize() > MAX_OWNER_VISIBLE_MEDIA_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_VIDEO_TYPES.contains(contentType))) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
    }
}


