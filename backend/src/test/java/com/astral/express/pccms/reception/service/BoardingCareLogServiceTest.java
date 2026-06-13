package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.request.CareLogRequest;
import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @Test
    void listBookings_shouldReturnList() {
        UUID id = UUID.randomUUID();
        Map<String, Object> row = Map.of(
                "id", id,
                "booking_code", "B001",
                "status_code", "RESERVED"
        );
        given(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).willReturn(List.of(row));

        List<BoardingBookingResponse> result = boardingCareLogService.listBookings(" Milu ", "");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(id);
        assertThat(result.get(0).bookingCode()).isEqualTo("B001");
        
        // Null status and keyword
        given(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).willReturn(List.of(row));
        List<BoardingBookingResponse> resultNull = boardingCareLogService.listBookings(null, null);
        assertThat(resultNull).hasSize(1);
    }

    @Test
    void listCareLogs_shouldReturnList() {
        UUID clId = UUID.randomUUID();
        Map<String, Object> row = Map.of(
                "id", clId,
                "pet_name", "Milu"
        );
        given(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).willReturn(List.of(row));

        List<CareLogResponse> result = boardingCareLogService.listCareLogs(UUID.randomUUID(), UUID.randomUUID());

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

        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("SELECT bs.id"), eq(petId)))
                .willReturn(Map.of("id", sessionId));
        given(securityContextService.getCurrentUserId()).willReturn(staffId);

        Map<String, Object> insertResult = Map.of("id", careLogId);
        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("INSERT INTO care_logs"), eq(sessionId), eq(petId), eq(staffId), any(), eq("MORNING"), eq("NORMAL"), eq("CLEAN"), eq("Fine"), eq(""))).willReturn(insertResult);

        Map<String, Object> selectResult = Map.of(
                "id", careLogId,
                "session_id", sessionId,
                "pet_id", petId,
                "pet_name", "Milu",
                "staff_name", "Staff A",
                "period_code", "MORNING"
        );
        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("WHERE cl.id = ?"), eq(careLogId))).willReturn(selectResult);

        CareLogResponse res = boardingCareLogService.saveCareLog(req);
        assertThat(res.id()).isEqualTo(careLogId);
    }

    @Test
    void uploadMedia_shouldThrowException_whenEmpty() {
        MockMultipartFile file = new MockMultipartFile("f", new byte[0]);
        assertThatThrownBy(() -> boardingCareLogService.uploadMedia(UUID.randomUUID(), file))
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
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn(null);
        org.mockito.Mockito.when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(file.getSize()).thenReturn(100L);
        org.mockito.Mockito.when(file.getContentType()).thenReturn("image/png");
        UploadedFileResponse uploaded = new UploadedFileResponse(fileId, "url", "key", "image/png", 100L);
        given(securityContextService.getCurrentUserId()).willReturn(userId);
        given(fileMediaService.uploadOwnerVisibleMedia(file, userId)).willReturn(uploaded);

        CareLogMediaResponse response = boardingCareLogService.uploadMedia(careLogId, file);
        assertThat(response.originalName()).isEqualTo("care-log-image");
    }

    @Test
    void saveCareLog_whenSessionIdProvidedButPetIdNot() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID careLogId = UUID.randomUUID();
        
        CareLogRequest req = new CareLogRequest(sessionId, null, "", "MORNING", "NORMAL", "CLEAN", "Fine", "");
        
        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("SELECT b.pet_id"), eq(sessionId)))
                .willReturn(Map.of("pet_id", petId));
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        
        Map<String, Object> insertResult = Map.of("id", careLogId);
        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("INSERT INTO care_logs"), eq(sessionId), eq(petId), any(), any(), eq("MORNING"), eq("NORMAL"), eq("CLEAN"), eq("Fine"), eq(""))).willReturn(insertResult);
        
        given(jdbcTemplate.queryForMap(ArgumentMatchers.contains("WHERE cl.id = ?"), eq(careLogId)))
                .willThrow(new EmptyResultDataAccessException(1));
                
        assertThatThrownBy(() -> boardingCareLogService.saveCareLog(req))
                .isInstanceOf(BusinessException.class);
    }
}
