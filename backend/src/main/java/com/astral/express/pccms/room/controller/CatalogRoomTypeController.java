package com.astral.express.pccms.room.controller;

import com.astral.express.pccms.room.dto.compatibility.CreateRoomTypeRequest;
import com.astral.express.pccms.room.dto.compatibility.UpdateRoomTypeRequest;
import com.astral.express.pccms.room.dto.compatibility.LegacyRoomTypeResponse;
import com.astral.express.pccms.room.security.RoomPermissions;
import com.astral.express.pccms.room.service.RoomAdminService;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/catalog/room-types")
@RequiredArgsConstructor
public class CatalogRoomTypeController {

    private final RoomAdminService roomAdminService;

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @PostMapping
    public ApiResponse<LegacyRoomTypeResponse> create(@Valid @RequestBody CreateRoomTypeRequest request) {
        RoomTypeRequest adminRequest = new RoomTypeRequest(
                request.code(),
                request.name(),
                request.defaultCapacity(),
                request.baseDailyPriceVnd() != null ? request.baseDailyPriceVnd().longValue() : 0L,
                request.description(),
                request.isActive()
        );
        return ApiResponse.created(toLegacyResponse(roomAdminService.createRoomType(adminRequest)));
    }

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @PutMapping("/{id}")
    public ApiResponse<LegacyRoomTypeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        RoomTypeRequest adminRequest = new RoomTypeRequest(
                request.code(),
                request.name(),
                request.defaultCapacity(),
                request.baseDailyPriceVnd() != null ? request.baseDailyPriceVnd().longValue() : 0L,
                request.description(),
                request.isActive()
        );
        return ApiResponse.success(toLegacyResponse(roomAdminService.updateRoomType(id, adminRequest)));
    }

    @PreAuthorize(RoomPermissions.ROOM_READ)
    @GetMapping("/{id}")
    public ApiResponse<LegacyRoomTypeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(toLegacyResponse(roomAdminService.getRoomType(id)));
    }

    @PreAuthorize(RoomPermissions.ROOM_READ)
    @GetMapping
    public ApiResponse<List<LegacyRoomTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<LegacyRoomTypeResponse> responses = roomAdminService.listRoomTypes(activeOnly).stream()
                .map(this::toLegacyResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @PreAuthorize(RoomPermissions.ROOM_MANAGE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roomAdminService.deactivateRoomType(id);
        return ApiResponse.success(null);
    }

    private LegacyRoomTypeResponse toLegacyResponse(RoomTypeResponse adminResponse) {
        return new LegacyRoomTypeResponse(
                adminResponse.id(),
                adminResponse.code(),
                adminResponse.name(),
                adminResponse.defaultCapacity(),
                adminResponse.baseDailyPriceVnd() != null ? BigDecimal.valueOf(adminResponse.baseDailyPriceVnd()) : BigDecimal.ZERO,
                adminResponse.description(),
                adminResponse.isActive()
        );
    }
}
