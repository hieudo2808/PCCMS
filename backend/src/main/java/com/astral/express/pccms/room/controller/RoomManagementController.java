package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomStatusUpdateRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.service.RoomManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/rooms")
@RequiredArgsConstructor
public class RoomManagementController {

    private final RoomManagementService roomManagementService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<PageResponse<RoomResponse>> searchRooms(
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) RoomStatus statusCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(roomManagementService.searchRooms(roomTypeId, statusCode, pageable));
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomResponse> getRoom(@PathVariable UUID roomId) {
        return ApiResponse.success(roomManagementService.getRoom(roomId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.success(roomManagementService.createRoom(request));
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomRequest request) {
        return ApiResponse.success(roomManagementService.updateRoom(roomId, request));
    }

    @PatchMapping("/{roomId}/status")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomResponse> updateRoomStatus(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomStatusUpdateRequest request) {
        return ApiResponse.success(roomManagementService.updateRoomStatus(roomId, request.statusCode()));
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasAuthority('ROOM_MANAGE')")
    public ApiResponse<RoomResponse> deactivateRoom(@PathVariable UUID roomId) {
        return ApiResponse.success(roomManagementService.deactivateRoom(roomId));
    }
}
