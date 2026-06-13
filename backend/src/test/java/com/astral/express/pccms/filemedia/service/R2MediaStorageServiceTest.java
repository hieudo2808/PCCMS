package com.astral.express.pccms.filemedia.service;

import com.astral.express.pccms.filemedia.service.CloudMediaStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class R2MediaStorageServiceTest {

    @Test
    void uploadImageStoresObjectInConfiguredBucketAndReturnsPublicUrl() {
        S3Client s3Client = mock(S3Client.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneOffset.UTC);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id",
                "access-key",
                "secret-key",
                "pccms-media",
                "https://media.example.com/",
                "public,max-age=60",
                s3Client,
                clock
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1, 2, 3});

        CloudMediaStorageService.StoredMedia storedMedia = storageService.uploadImage(file, "pccms/care-logs");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("pccms-media");
        assertThat(request.key()).startsWith("pccms/care-logs/2026/06/10/");
        assertThat(request.key()).endsWith(".png");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(3L);
        assertThat(request.cacheControl()).isEqualTo("public,max-age=60");
        assertThat(storedMedia.publicId()).isEqualTo(request.key());
        assertThat(storedMedia.secureUrl()).isEqualTo("https://media.example.com/" + request.key());
        assertThat(storedMedia.mimeType()).isEqualTo("image/png");
        assertThat(storedMedia.sizeBytes()).isEqualTo(3L);
    }

    @Test
    void uploadImage_shouldUseDefaultMimeType_whenContentTypeIsNull() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", null, new byte[]{1});
        CloudMediaStorageService.StoredMedia storedMedia = storageService.uploadImage(file, null);
        assertThat(storedMedia.mimeType()).isEqualTo("image/jpeg");
    }

    @Test
    void uploadImage_shouldThrowBusinessException_whenS3ClientThrowsException() {
        S3Client s3Client = mock(S3Client.class);
        org.mockito.Mockito.doThrow(new RuntimeException("S3 Error")).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1});
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> storageService.uploadImage(file, ""))
                .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void constructor_shouldCreateS3ClientAndUseSystemClock_whenNullsProvided() {
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, null, null
        );
        assertThat(storageService).isNotNull();
    }

    @Test
    void constructor_shouldThrowException_whenRequiredConfigMissing() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "", "https://media.example.com/", null, null, null
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uploadImage_shouldNormalizeFolder_whenFolderHasSlashes() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "//pccms/care-logs//");
        assertThat(media.publicId()).startsWith("pccms/care-logs/");
    }

    @Test
    void uploadImage_shouldExtractExtension_whenOriginalNameIsValid() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.JPEG", "image/jpeg", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "");
        assertThat(media.publicId()).endsWith(".jpeg");
    }

    @Test
    void uploadImage_shouldFallbackToContentType_whenOriginalNameHasNoExtension() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet", "image/webp", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "");
        assertThat(media.publicId()).endsWith(".webp");
    }

    @Test
    void uploadImage_shouldFallbackToBin_whenOriginalNameHasNoExtensionAndContentTypeUnknown() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet", "application/pdf", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "");
        assertThat(media.publicId()).endsWith(".bin");
    }

    @Test
    void uploadImage_shouldFallbackToContentType_whenOriginalNameIsNull() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        try {
            given(mockFile.getBytes()).willReturn(new byte[]{1});
        } catch(Exception e) {}
        given(mockFile.getContentType()).willReturn("image/jpeg");
        given(mockFile.getOriginalFilename()).willReturn(null);

        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(mockFile, "");
        assertThat(media.publicId()).endsWith(".jpg");
    }


    @Test
    void uploadImage_shouldNormalizeFolder_whenFolderIsExactlyPccms() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "pccms");
        // Normalized is "", prefix is "pccms"
        assertThat(media.publicId()).startsWith("pccms/");
    }

    @Test
    void uploadImage_shouldNormalizeFolder_whenFolderDoesNotStartWithPccms() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "some-folder");
        assertThat(media.publicId()).startsWith("pccms/some-folder/");
    }

    @Test
    void uploadImage_shouldFallbackToContentType_whenOriginalNameHasDotAtEnd() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.", "image/png", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "");
        assertThat(media.publicId()).endsWith(".png");
    }

    @Test
    void uploadImage_shouldFallbackToContentType_whenOriginalNameExtensionIsTooLong() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", null, s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.thisisaverylongextension", "image/jpeg", new byte[]{1});
        CloudMediaStorageService.StoredMedia media = storageService.uploadImage(file, "");
        assertThat(media.publicId()).endsWith(".jpg");
    }

    @Test
    void uploadImage_shouldUseProvidedCacheControl() {
        S3Client s3Client = mock(S3Client.class);
        R2MediaStorageService storageService = new R2MediaStorageService(
                "account-id", "access-key", "secret-key", "pccms-media", "https://media.example.com/", "public,max-age=123", s3Client, null
        );
        MockMultipartFile file = new MockMultipartFile("file", "pet.png", "image/png", new byte[]{1});
        storageService.uploadImage(file, "");
        
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().cacheControl()).isEqualTo("public,max-age=123");
    }

}
