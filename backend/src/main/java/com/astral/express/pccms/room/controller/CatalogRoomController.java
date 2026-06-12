package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.room.dto.compatibility.CreateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.UpdateRoomRequest;
import com.astral.express.pccms.room.dto.compatibility.LegacyRoomResponse;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.security.RoomPermissions;
import com.astral.express.pccms.room.service.compatibility.CatalogRoomService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/catalog/rooms")
@RequiredArgsConstructor
public class CatalogRoomController {

    private final CatalogRoomService CatalogRoomService;

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @PostMapping
    public ApiResponse<LegacyRoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.created(CatalogRoomService.create(request));
    }

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @PutMapping("/{id}")
    public ApiResponse<LegacyRoomResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ApiResponse.success(CatalogRoomService.update(id, request));
    }

    @PreAuthorize(RoomPermissions.ROOM_READ)
    @GetMapping("/{id}")
    public ApiResponse<LegacyRoomResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(CatalogRoomService.getById(id));
    }

    @PreAuthorize(RoomPermissions.ROOM_READ)
    @GetMapping
    public ApiResponse<PageResponse<LegacyRoomResponse>> list(
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) RoomStatus statusCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(CatalogRoomService.list(roomTypeId, statusCode, pageable));
    }

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        CatalogRoomService.delete(id);
        return ApiResponse.success(null);
    }
}


