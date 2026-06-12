package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.room.dto.request.RoomTypeActiveRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.service.RoomAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/room-types")
@RequiredArgsConstructor
public class RoomTypeManagementController {
    private final RoomAdminService roomAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<List<RoomTypeResponse>> listRoomTypes(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(roomAdminService.listRoomTypes(activeOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomTypeResponse> getRoomType(@PathVariable UUID id) {
        return ApiResponse.success(roomAdminService.getRoomType(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomTypeResponse> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.created(roomAdminService.createRoomType(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomTypeResponse> updateRoomType(
            @PathVariable UUID id,
            @Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.success(roomAdminService.updateRoomType(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomTypeResponse> updateRoomTypeActive(
            @PathVariable UUID id,
            @Valid @RequestBody RoomTypeActiveRequest request) {
        return ApiResponse.success(roomAdminService.updateRoomTypeActive(id, request.isActive()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<Void> deactivateRoomType(@PathVariable UUID id) {
        roomAdminService.deactivateRoomType(id);
        return ApiResponse.success(null);
    }
}
