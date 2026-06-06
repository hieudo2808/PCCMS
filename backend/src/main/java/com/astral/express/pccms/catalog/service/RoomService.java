package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.catalog.dto.request.CreateRoomRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomRequest;
import com.astral.express.pccms.catalog.dto.response.RoomResponse;
import com.astral.express.pccms.catalog.entity.RoomStatus;
import com.astral.express.pccms.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoomService {
    RoomResponse create(CreateRoomRequest request);
    RoomResponse update(UUID id, UpdateRoomRequest request);
    RoomResponse getById(UUID id);
    PageResponse<RoomResponse> list(UUID roomTypeId, RoomStatus statusCode, Pageable pageable);
    void delete(UUID id);
}
