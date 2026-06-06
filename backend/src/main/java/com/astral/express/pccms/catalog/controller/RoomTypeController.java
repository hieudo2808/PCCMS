package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.catalog.dto.request.CreateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.response.RoomTypeResponse;
import com.astral.express.pccms.catalog.security.CatalogPermissions;
import com.astral.express.pccms.catalog.service.RoomTypeService;
import com.astral.express.pccms.common.dto.ApiResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/catalog/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @PostMapping
    public ApiResponse<RoomTypeResponse> create(@Valid @RequestBody CreateRoomTypeRequest request) {
        return ApiResponse.created(roomTypeService.create(request));
    }

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @PutMapping("/{id}")
    public ApiResponse<RoomTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        return ApiResponse.success(roomTypeService.update(id, request));
    }

    @PreAuthorize(CatalogPermissions.ROOM_READ)
    @GetMapping("/{id}")
    public ApiResponse<RoomTypeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(roomTypeService.getById(id));
    }

    @PreAuthorize(CatalogPermissions.ROOM_READ)
    @GetMapping
    public ApiResponse<List<RoomTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(activeOnly ? roomTypeService.listActive() : roomTypeService.listAll());
    }

    @PreAuthorize(CatalogPermissions.ROOM_MANAGE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roomTypeService.delete(id);
        return ApiResponse.success(null);
    }
}
