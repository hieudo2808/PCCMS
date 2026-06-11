package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.reception.dto.request.CareLogRequest;
import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import com.astral.express.pccms.reception.service.BoardingCareLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingCareLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingCareLogService boardingCareLogService;

    @InjectMocks
    private BoardingCareLogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(get("/").contextPath("/api"))
                .build();
    }

    @Test
    @DisplayName("API-REC-008: List bookings successfully")
    void should_return_list_of_bookings() throws Exception {
        UUID id = UUID.randomUUID();
        BoardingBookingResponse response = new BoardingBookingResponse(
                id, "B-123", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                "CONFIRMED", "Needs diet", null, "Rex", "Owner", "0123456789", "VIP", null, null
        );

        given(boardingCareLogService.listBookings(anyString(), isNull())).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reception/boarding/bookings")
                        .contextPath("/api")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách lưu trú thành công"))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].bookingCode").value("B-123"));

        verify(boardingCareLogService).listBookings("", null);
    }

    @Test
    @DisplayName("API-REC-009: List care logs successfully")
    void should_return_list_of_care_logs() throws Exception {
        UUID id = UUID.randomUUID();
        CareLogResponse response = new CareLogResponse(
                id, UUID.randomUUID(), UUID.randomUUID(), "Rex", "Staff A",
                java.time.LocalDate.now(), "MORNING", "GOOD", "CLEAN", "Healthy", "Played well", new Timestamp(System.currentTimeMillis())
        );

        given(boardingCareLogService.listCareLogs(isNull(), isNull())).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reception/boarding/care-logs")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lấy nhật ký lưu trú thành công"))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].petName").value("Rex"));

        verify(boardingCareLogService).listCareLogs(null, null);
    }

    @Test
    @DisplayName("API-REC-010: Save care log successfully")
    void should_save_care_log_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        CareLogResponse response = new CareLogResponse(
                id, UUID.randomUUID(), UUID.randomUUID(), "Rex", "Staff A",
                java.time.LocalDate.now(), "MORNING", "GOOD", "CLEAN", "Healthy", "Played well", new Timestamp(System.currentTimeMillis())
        );

        given(boardingCareLogService.saveCareLog(any(CareLogRequest.class))).willReturn(response);

        String payload = """
                {
                  "periodCode": "MORNING",
                  "feedingStatus": "GOOD",
                  "hygieneStatus": "CLEAN",
                  "healthNote": "Healthy"
                }
                """;

        mockMvc.perform(post("/api/v1/reception/boarding/care-logs")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lưu nhật ký lưu trú thành công"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.petName").value("Rex"));

        verify(boardingCareLogService).saveCareLog(any(CareLogRequest.class));
    }

    @Test
    @DisplayName("API-REC-011: Save care log fails on validation")
    void should_return_400_when_care_log_validation_fails() throws Exception {
        String payload = """
                {
                  "feedingStatus": "GOOD"
                }
                """; // missing periodCode, hygieneStatus

        mockMvc.perform(post("/api/v1/reception/boarding/care-logs")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(boardingCareLogService);
    }

    @Test
    @DisplayName("API-REC-012: Save care log fails - Invalid care log (Missing session/pet)")
    void should_return_400_when_care_log_is_invalid() throws Exception {
        given(boardingCareLogService.saveCareLog(any(CareLogRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG));

        String payload = """
                {
                  "periodCode": "MORNING",
                  "feedingStatus": "GOOD",
                  "hygieneStatus": "CLEAN"
                }
                """;

        mockMvc.perform(post("/api/v1/reception/boarding/care-logs")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("ERR_REC_005_INVALID_CARE_LOG"));
    }

    @Test
    @DisplayName("API-REC-013: Upload care log media successfully")
    void should_upload_care_log_media_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        CareLogMediaResponse response = new CareLogMediaResponse(
                fileId, "image.jpg", "/media/image.jpg", "image/jpeg", 1024L, UUID.randomUUID(), "OWNER_VISIBLE"
        );

        given(boardingCareLogService.uploadMedia(eq(id), any(MultipartFile.class))).willReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/reception/boarding/care-logs/{id}/media", id)
                        .file(file)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tải ảnh/video thành công"))
                .andExpect(jsonPath("$.data.id").value(fileId.toString()))
                .andExpect(jsonPath("$.data.storedKey").value("/media/image.jpg"));

        verify(boardingCareLogService).uploadMedia(eq(id), any(MultipartFile.class));
    }
}
