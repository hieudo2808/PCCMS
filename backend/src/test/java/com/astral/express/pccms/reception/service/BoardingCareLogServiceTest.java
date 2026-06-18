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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardingCareLogServiceTest {

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private FileMediaService fileMediaService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private BoardingCareLogQueryRepository boardingCareLogQueryRepository;
    @Mock
    private BoardingCareLogCommandRepository boardingCareLogCommandRepository;

    private BoardingCareLogService boardingCareLogService;

    @BeforeEach
    void setUp() {
        boardingCareLogService = new BoardingCareLogService(
                securityContextService,
                fileMediaService,
                boardingCareLogQueryRepository,
                boardingCareLogCommandRepository);
    }

    @Test
    void uploadMediaUsesFileMediaServiceInsteadOfWritingFakeFileAssetKey() {
        UUID careLogId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        MediaUploadCommand media = new MediaUploadCommand("care.png", "image/png", new byte[]{1, 2, 3});
        UploadedFileResponse uploaded = new UploadedFileResponse(
                fileId,
                "https://media.example.com/pccms/care-logs/2026/06/10/object.png",
                "pccms/care-logs/2026/06/10/object.png",
                "image/png",
                3L
        );
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(boardingCareLogCommandRepository.canEditCareLog(careLogId, userId)).willReturn(Optional.of(true));
        given(fileMediaService.uploadOwnerVisibleMedia(media, userId)).willReturn(uploaded);

        CareLogMediaResponse response = boardingCareLogService.uploadMedia(careLogId, media);

        verify(fileMediaService).uploadOwnerVisibleMedia(media, userId);
        verify(boardingCareLogCommandRepository).insertCareLogMedia(eq(careLogId), eq(fileId), any());
        assertThat(response.id()).isEqualTo(fileId);
        assertThat(response.originalName()).isEqualTo("care.png");
        assertThat(response.storedKey()).isEqualTo(uploaded.url());
        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(3L);
        assertThat(response.uploadedBy()).isEqualTo(userId);
        assertThat(response.visibilityCode()).isEqualTo("OWNER_VISIBLE");
    }

    @Test
    void listBookings_shouldReturnList() {
        UUID id = UUID.randomUUID();
        BoardingBookingResponse row = new BoardingBookingResponse(
                id, "B001", null, null, "RESERVED", null, null,
                null, null, null, null, null, null);
        given(boardingCareLogQueryRepository.listBookings(" Milu ", "")).willReturn(List.of(row));

        List<BoardingBookingResponse> result = boardingCareLogService.listBookings(" Milu ", "");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).bookingCode()).isEqualTo("B001");
        
        // Null status and keyword
        given(boardingCareLogQueryRepository.listBookings(null, null)).willReturn(List.of(row));
        List<BoardingBookingResponse> resultNull = boardingCareLogService.listBookings(null, null);
        assertThat(resultNull).hasSize(1);
    }

    @Test
    void listCareLogs_shouldReturnList() {
        UUID clId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        CareLogResponse row = new CareLogResponse(clId, sessionId, petId, "Milu", null, null, null, null, null, null, null, null, false, false, null, null);
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(boardingCareLogQueryRepository.listCareLogs(userId, sessionId, petId)).willReturn(List.of(row));

        List<CareLogResponse> result = boardingCareLogService.listCareLogs(sessionId, petId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(clId);
        assertThat(result.get(0).petName()).isEqualTo("Milu");
    }

    @Test
    void saveCareLog_shouldThrowException_whenInvalid() {
        CareLogRequest req = new CareLogRequest(null, null, "2026-06-12", "MORNING", "NORMAL", "CLEAN", "Fine", "");
        assertThatThrownBy(() -> boardingCareLogService.saveCareLog(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
    }

    @Test
    void saveCareLog_shouldResolveSessionIdAndInsert() {
        UUID petId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID careLogId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CareLogRequest req = new CareLogRequest(null, petId, LocalDate.now().toString(), "MORNING", "NORMAL", "CLEAN", "Fine", "");

        given(boardingCareLogCommandRepository.findLatestActiveSessionIdByPet(petId)).willReturn(Optional.of(sessionId));
        given(securityContextService.getCurrentUserId()).willReturn(staffId);
        given(boardingCareLogCommandRepository.findExistingCareLogId(eq(sessionId), any(LocalDate.class), eq("MORNING")))
                .willReturn(Optional.empty());
        given(boardingCareLogCommandRepository.findActiveWorkScheduleId(eq(staffId), any(LocalDate.class)))
                .willReturn(Optional.of(UUID.randomUUID()));

        given(boardingCareLogCommandRepository.createCareLog(eq(sessionId), eq(petId), eq(staffId), any(UUID.class), any(), eq("MORNING"), eq("NORMAL"), eq("CLEAN"), eq("Fine"), eq(""))).willReturn(careLogId);

        given(boardingCareLogQueryRepository.findCareLog(staffId, careLogId)).willReturn(Optional.of(new CareLogResponse(
                careLogId, sessionId, petId, "Milu", "Staff A", null, "MORNING",
                null, null, null, null, null, false, false, null, null)));

        CareLogResponse res = boardingCareLogService.saveCareLog(req);
        assertThat(res.id()).isEqualTo(careLogId);
    }

    @Test
    void uploadMedia_shouldThrowException_whenEmpty() {
        MediaUploadCommand media = new MediaUploadCommand("empty.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> boardingCareLogService.uploadMedia(UUID.randomUUID(), media))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
    }

    @Test
    void uploadMedia_shouldThrowException_whenNullFile() {
        assertThatThrownBy(() -> boardingCareLogService.uploadMedia(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void uploadMedia_withNullFilename() {
        UUID careLogId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        MediaUploadCommand media = new MediaUploadCommand(null, "image/png", new byte[100]);
        UploadedFileResponse uploaded = new UploadedFileResponse(fileId, "url", "key", "image/png", 100L);
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(boardingCareLogCommandRepository.canEditCareLog(careLogId, userId)).willReturn(Optional.of(true));
        given(fileMediaService.uploadOwnerVisibleMedia(media, userId)).willReturn(uploaded);

        CareLogMediaResponse response = boardingCareLogService.uploadMedia(careLogId, media);
        assertThat(response.originalName()).isEqualTo("care-log-image");
    }

    @Test
    void saveCareLog_whenSessionIdProvidedButPetIdNot() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID careLogId = UUID.randomUUID();
        
        CareLogRequest req = new CareLogRequest(sessionId, null, "", "MORNING", "NORMAL", "CLEAN", "Fine", "");
        
        given(boardingCareLogCommandRepository.findPetIdBySession(sessionId)).willReturn(petId);
        UUID staffId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(staffId);
        given(boardingCareLogCommandRepository.findExistingCareLogId(eq(sessionId), any(LocalDate.class), eq("MORNING")))
                .willReturn(Optional.empty());
        given(boardingCareLogCommandRepository.findActiveWorkScheduleId(eq(staffId), any(LocalDate.class)))
                .willReturn(Optional.of(UUID.randomUUID()));
        
        given(boardingCareLogCommandRepository.createCareLog(eq(sessionId), eq(petId), any(), any(), any(), eq("MORNING"), eq("NORMAL"), eq("CLEAN"), eq("Fine"), eq(""))).willReturn(careLogId);
        
        given(boardingCareLogQueryRepository.findCareLog(staffId, careLogId)).willReturn(Optional.empty());
                
        assertThatThrownBy(() -> boardingCareLogService.saveCareLog(req))
                .isInstanceOf(BusinessException.class);
    }
}
