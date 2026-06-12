package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomStatusUpdateRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.service.RoomAdminService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class RoomAdminController {

    private final RoomAdminService roomAdminService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/room-types")
    public ApiResponse<List<RoomTypeResponse>> listActiveRoomTypes() {
        return ApiResponse.success(roomAdminService.listActiveRoomTypes());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/room-types")
    public ApiResponse<RoomTypeResponse> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.created(roomAdminService.createRoomType(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/room-types/{id}")
    public ApiResponse<RoomTypeResponse> updateRoomType(@PathVariable UUID id, @Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.success(roomAdminService.updateRoomType(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/room-types/{id}")
    public ApiResponse<Void> deactivateRoomType(@PathVariable UUID id) {
        roomAdminService.deactivateRoomType(id);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @GetMapping("/rooms")
    public ApiResponse<PageResponse<RoomResponse>> listRooms(
            @PageableDefault(size = 20, sort = "roomCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(roomAdminService.listRooms(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rooms")
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.created(roomAdminService.createRoom(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/rooms/{id}")
    public ApiResponse<RoomResponse> updateRoom(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.success(roomAdminService.updateRoom(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/rooms/{id}/status")
    public ApiResponse<RoomResponse> updateRoomStatus(@PathVariable UUID id, @Valid @RequestBody RoomStatusUpdateRequest request) {
        return ApiResponse.success(roomAdminService.updateRoomStatus(id, request));
    }
}
