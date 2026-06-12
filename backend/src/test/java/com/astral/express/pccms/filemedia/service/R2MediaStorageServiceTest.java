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
}

