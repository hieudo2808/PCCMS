package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardingCareLogServiceTest {

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private FileMediaService fileMediaService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private BoardingCareLogService boardingCareLogService;

    @BeforeEach
    void setUp() {
        boardingCareLogService = new BoardingCareLogService(jdbcTemplate, securityContextService, fileMediaService);
    }

    @Test
    void uploadMediaUsesFileMediaServiceInsteadOfWritingFakeFileAssetKey() {
        UUID careLogId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "care.png", "image/png", new byte[]{1, 2, 3});
        UploadedFileResponse uploaded = new UploadedFileResponse(
                fileId,
                "https://media.example.com/pccms/care-logs/2026/06/10/object.png",
                "pccms/care-logs/2026/06/10/object.png",
                "image/png",
                3L
        );
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(fileMediaService.uploadOwnerVisibleMedia(file, userId)).willReturn(uploaded);

        CareLogMediaResponse response = boardingCareLogService.uploadMedia(careLogId, file);

        verify(fileMediaService).uploadOwnerVisibleMedia(file, userId);
        verify(jdbcTemplate).update(
                eq("INSERT INTO care_log_media(care_log_id, file_id, caption) VALUES (?, ?, ?)"),
                eq(careLogId),
                eq(fileId),
                any()
        );
        assertThat(response.id()).isEqualTo(fileId);
        assertThat(response.originalName()).isEqualTo("care.png");
        assertThat(response.storedKey()).isEqualTo(uploaded.url());
        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(3L);
        assertThat(response.uploadedBy()).isEqualTo(userId);
        assertThat(response.visibilityCode()).isEqualTo("OWNER_VISIBLE");
    }
}


