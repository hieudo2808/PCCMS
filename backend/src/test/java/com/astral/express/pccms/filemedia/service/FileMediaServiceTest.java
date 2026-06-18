package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.entity.FileVisibility;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import java.io.IOException;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class FileMediaServiceTest {

    @Mock
    private CloudMediaStorageService cloudMediaStorageService;
    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private UserRepository userRepository;

    private FileMediaService fileMediaService;

    @BeforeEach
    void setUp() {
        fileMediaService = new FileMediaService(cloudMediaStorageService, fileAssetRepository, userRepository);
    }

    @Test
    void uploadOwnerVisibleImageStoresR2PublicUrlInFileAsset() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Users uploader = Users.builder()
                .id(userId)
                .fullName("Staff")
                .email("staff@example.com")
                .passwordHash("hash")
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "pet.jpg", "image/jpeg", new byte[]{1, 2, 3});
        CloudMediaStorageService.StoredMedia storedMedia = new CloudMediaStorageService.StoredMedia(
                "pccms/care-logs/2026/06/10/object.jpg",
                "https://media.example.com/pccms/care-logs/2026/06/10/object.jpg",
                "image/jpeg",
                3L
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(uploader));
        given(cloudMediaStorageService.store(any(StoreMediaCommand.class))).willReturn(storedMedia);
        given(fileAssetRepository.save(any(FileAsset.class))).willAnswer(invocation -> {
            FileAsset asset = invocation.getArgument(0);
            asset.setId(fileId);
            return asset;
        });

        UploadedFileResponse response = fileMediaService.uploadOwnerVisibleImage(file, userId);

        ArgumentCaptor<FileAsset> assetCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetRepository).save(assetCaptor.capture());
        ArgumentCaptor<StoreMediaCommand> commandCaptor = ArgumentCaptor.forClass(StoreMediaCommand.class);
        verify(cloudMediaStorageService).store(commandCaptor.capture());
        StoreMediaCommand command = commandCaptor.getValue();
        FileAsset savedAsset = assetCaptor.getValue();
        assertThat(command.folder()).isEqualTo("care-logs");
        assertThat(command.originalFilename()).isEqualTo("pet.jpg");
        assertThat(command.contentType()).isEqualTo("image/jpeg");
        assertThat(command.bytes()).containsExactly(1, 2, 3);
        assertThat(savedAsset.getStoredKey()).isEqualTo(storedMedia.secureUrl());
        assertThat(savedAsset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(savedAsset.getSizeBytes()).isEqualTo(3L);
        assertThat(savedAsset.getUploadedBy()).isSameAs(uploader);
        assertThat(savedAsset.getVisibilityCode()).isEqualTo(FileVisibility.OWNER_VISIBLE);
        assertThat(response.id()).isEqualTo(fileId);
        assertThat(response.url()).isEqualTo(storedMedia.secureUrl());
        assertThat(response.publicId()).isEqualTo(storedMedia.publicId());
    }

    @Test
    void uploadOwnerVisibleMediaAcceptsCareLogVideo() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Users uploader = Users.builder()
                .id(userId)
                .fullName("Staff")
                .email("staff@example.com")
                .passwordHash("hash")
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "care.mp4", "video/mp4", new byte[]{1, 2, 3});
        CloudMediaStorageService.StoredMedia storedMedia = new CloudMediaStorageService.StoredMedia(
                "pccms/care-logs/2026/06/10/object.mp4",
                "https://media.example.com/pccms/care-logs/2026/06/10/object.mp4",
                "video/mp4",
                3L
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(uploader));
        given(cloudMediaStorageService.store(any(StoreMediaCommand.class))).willReturn(storedMedia);
        given(fileAssetRepository.save(any(FileAsset.class))).willAnswer(invocation -> {
            FileAsset asset = invocation.getArgument(0);
            asset.setId(fileId);
            return asset;
        });

        UploadedFileResponse response = fileMediaService.uploadOwnerVisibleMedia(file, userId);

        assertThat(response.id()).isEqualTo(fileId);
        assertThat(response.url()).isEqualTo(storedMedia.secureUrl());
        assertThat(response.mimeType()).isEqualTo("video/mp4");
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "pet.jpg", "image/jpeg", new byte[]{1, 2, 3});
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(file, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void uploadOwnerVisibleImage_shouldHandleNullOriginalFilename() throws IOException {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Users uploader = Users.builder().id(userId).build();
        
        MultipartFile file = Mockito.mock(MultipartFile.class);
        given(file.getOriginalFilename()).willReturn(null);
        given(file.getSize()).willReturn(3L);
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.isEmpty()).willReturn(false);
        given(file.getBytes()).willReturn(new byte[] {1, 2, 3});
        
        CloudMediaStorageService.StoredMedia storedMedia = new CloudMediaStorageService.StoredMedia(
                "pccms/care-logs/2026/06/10/object.jpg",
                "https://media.example.com/pccms/care-logs/2026/06/10/object.jpg",
                "image/jpeg",
                3L
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(uploader));
        given(cloudMediaStorageService.store(any(StoreMediaCommand.class))).willReturn(storedMedia);
        given(fileAssetRepository.save(any(FileAsset.class))).willAnswer(inv -> {
            FileAsset a = inv.getArgument(0);
            a.setId(fileId);
            return a;
        });

        UploadedFileResponse response = fileMediaService.uploadOwnerVisibleImage(file, userId);
        
        ArgumentCaptor<FileAsset> assetCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getOriginalName()).isEqualTo("care-log-image");
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenFileIsNull() {
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(
                (MultipartFile) null,
                UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "pet.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenFileTooLarge() {
        byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "pet.jpg", "image/jpeg", largeContent);
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenContentTypeIsNull() {
        MockMultipartFile file = new MockMultipartFile("file", "pet.jpg", null, new byte[]{1,2});
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleImage_shouldThrowException_whenContentTypeIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "pet.txt", "text/plain", new byte[]{1,2});
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleImage(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleMedia_shouldThrowException_whenFileIsNull() {
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleMedia((MultipartFile) null, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleMedia_shouldThrowException_whenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "care.mp4", "video/mp4", new byte[0]);
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleMedia(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleMedia_shouldThrowException_whenFileTooLarge() {
        byte[] largeContent = new byte[20 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "care.mp4", "video/mp4", largeContent);
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleMedia(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleMedia_shouldThrowException_whenContentTypeIsNull() {
        MockMultipartFile file = new MockMultipartFile("file", "care.mp4", null, new byte[]{1,2});
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleMedia(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @Test
    void uploadOwnerVisibleMedia_shouldThrowException_whenContentTypeIsInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "care.txt", "text/plain", new byte[]{1,2});
        assertThatThrownBy(() -> fileMediaService.uploadOwnerVisibleMedia(file, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

}
