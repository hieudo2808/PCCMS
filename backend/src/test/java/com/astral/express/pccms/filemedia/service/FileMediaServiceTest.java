package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.entity.FileVisibility;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.filemedia.service.CloudMediaStorageService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
        given(cloudMediaStorageService.uploadImage(any(), eq("care-logs"))).willReturn(storedMedia);
        given(fileAssetRepository.save(any(FileAsset.class))).willAnswer(invocation -> {
            FileAsset asset = invocation.getArgument(0);
            asset.setId(fileId);
            return asset;
        });

        UploadedFileResponse response = fileMediaService.uploadOwnerVisibleImage(file, userId);

        ArgumentCaptor<FileAsset> assetCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetRepository).save(assetCaptor.capture());
        FileAsset savedAsset = assetCaptor.getValue();
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
        given(cloudMediaStorageService.uploadImage(any(), eq("care-logs"))).willReturn(storedMedia);
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
}


