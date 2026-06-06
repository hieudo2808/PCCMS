package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.catalog.dto.request.CreateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.response.RoomTypeResponse;

import java.util.List;
import java.util.UUID;

public interface RoomTypeService {
    RoomTypeResponse create(CreateRoomTypeRequest request);
    RoomTypeResponse update(UUID id, UpdateRoomTypeRequest request);
    RoomTypeResponse getById(UUID id);
    List<RoomTypeResponse> listActive();
    List<RoomTypeResponse> listAll();
    void delete(UUID id);
}
