package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.service.MediaUploadCommand;
import com.astral.express.pccms.reception.dto.request.CareLogRequest;
import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import com.astral.express.pccms.reception.service.BoardingCareLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reception/boarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class BoardingCareLogController {
    private final BoardingCareLogService boardingCareLogService;

    @GetMapping("/bookings")
    public ApiResponse<List<BoardingBookingResponse>> listBookings(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(boardingCareLogService.listBookings(q, status), "Lấy danh sách lưu trú thành công");
    }

    @GetMapping("/care-logs")
    public ApiResponse<List<CareLogResponse>> listCareLogs(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) UUID petId) {
        return ApiResponse.success(boardingCareLogService.listCareLogs(sessionId, petId), "Lấy nhật ký lưu trú thành công");
    }

    @PostMapping("/care-logs")
    public ApiResponse<CareLogResponse> saveCareLog(@Valid @RequestBody CareLogRequest request) {
        return ApiResponse.success(boardingCareLogService.saveCareLog(request), "Lưu nhật ký lưu trú thành công");
    }

    @GetMapping("/care-logs/{id}")
    public ApiResponse<CareLogResponse> getCareLog(@PathVariable UUID id) {
        return ApiResponse.success(boardingCareLogService.getCareLog(id), "Lay chi tiet nhat ky luu tru thanh cong");
    }

    @PutMapping("/care-logs/{id}")
    public ApiResponse<CareLogResponse> updateCareLog(@PathVariable UUID id, @Valid @RequestBody CareLogRequest request) {
        return ApiResponse.success(boardingCareLogService.updateCareLog(id, request), "Cap nhat nhat ky luu tru thanh cong");
    }

    @DeleteMapping("/care-logs/{id}")
    public ApiResponse<Void> deleteCareLog(@PathVariable UUID id) {
        boardingCareLogService.deleteCareLog(id);
        return ApiResponse.success(null, "Xoa nhat ky luu tru thanh cong");
    }

    @PostMapping("/care-logs/{id}/media")
    public ApiResponse<CareLogMediaResponse> uploadMedia(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(boardingCareLogService.uploadMedia(id, toMediaCommand(file)), "Tải ảnh/video thành công");
    }

    private MediaUploadCommand toMediaCommand(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        try {
            return new MediaUploadCommand(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE);
        }
    }
}
