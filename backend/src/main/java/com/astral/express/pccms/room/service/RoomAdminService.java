package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomStatusUpdateRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RoomAdminService {
    RoomTypeResponse createRoomType(RoomTypeRequest request);

    RoomTypeResponse updateRoomType(UUID id, RoomTypeRequest request);

    void deactivateRoomType(UUID id);

    List<RoomTypeResponse> listActiveRoomTypes();

    PageResponse<RoomResponse> listRooms(Pageable pageable);

    RoomResponse createRoom(RoomRequest request);

    RoomResponse updateRoom(UUID id, RoomRequest request);

    RoomResponse updateRoomStatus(UUID id, RoomStatusUpdateRequest request);
}
