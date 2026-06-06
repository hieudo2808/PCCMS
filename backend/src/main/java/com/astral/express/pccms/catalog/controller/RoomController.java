package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.catalog.dto.request.CreateRoomRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomRequest;
import com.astral.express.pccms.catalog.dto.response.RoomResponse;
import com.astral.express.pccms.catalog.entity.RoomStatus;
import com.astral.express.pccms.catalog.security.CatalogPermissions;
import com.astral.express.pccms.catalog.service.RoomService;
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
public class RoomController {

    private final RoomService roomService;

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @PostMapping
    public ApiResponse<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.created(roomService.create(request));
    }

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ApiResponse.success(roomService.update(id, request));
    }

    @PreAuthorize(CatalogPermissions.ROOM_READ)
    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(roomService.getById(id));
    }

    @PreAuthorize(CatalogPermissions.ROOM_READ)
    @GetMapping
    public ApiResponse<PageResponse<RoomResponse>> list(
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) RoomStatus statusCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(roomService.list(roomTypeId, statusCode, pageable));
    }

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roomService.delete(id);
        return ApiResponse.success(null);
    }
}
