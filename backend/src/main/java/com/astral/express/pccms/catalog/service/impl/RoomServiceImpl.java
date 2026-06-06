package com.astral.express.pccms.catalog.service.impl;

import com.astral.express.pccms.appointment.entity.RoomType;
import com.astral.express.pccms.appointment.repository.RoomTypeRepository;
import com.astral.express.pccms.catalog.dto.request.CreateRoomRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomRequest;
import com.astral.express.pccms.catalog.dto.response.RoomResponse;
import com.astral.express.pccms.catalog.entity.Room;
import com.astral.express.pccms.catalog.entity.RoomStatus;
import com.astral.express.pccms.catalog.repository.RoomRepository;
import com.astral.express.pccms.catalog.service.RoomService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public RoomResponse create(CreateRoomRequest request) {
        ensureUniqueCode(request.roomCode(), null);
        ensureUniqueName(request.name(), null);

        Room room = new Room();
        applyRequest(room, request);
        return toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(UUID id, UpdateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_NOT_FOUND));

        ensureUniqueCode(request.roomCode(), id);
        ensureUniqueName(request.name(), id);

        applyRequest(room, request);
        return toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getById(UUID id) {
        return roomRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(UUID roomTypeId, RoomStatus statusCode, Pageable pageable) {
        Page<Room> page;
        if (roomTypeId != null && statusCode != null) {
            page = roomRepository.findByRoomTypeIdAndStatusCode(roomTypeId, statusCode, pageable);
        } else if (roomTypeId != null) {
            page = roomRepository.findByRoomTypeId(roomTypeId, pageable);
        } else if (statusCode != null) {
            page = roomRepository.findByStatusCode(statusCode, pageable);
        } else {
            page = roomRepository.findAll(pageable);
        }
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_NOT_FOUND));
        if (room.getStatusCode() == RoomStatus.OCCUPIED) {
            throw new BusinessException(ErrorCode.ERR_ROOM_006_ROOM_OCCUPIED);
        }
        room.setStatusCode(RoomStatus.INACTIVE);
        roomRepository.save(room);
    }

    private void applyRequest(Room room, CreateRoomRequest request) {
        room.setRoomCode(request.roomCode());
        room.setName(request.name());
        room.setRoomType(resolveRoomType(request.roomTypeId()));
        room.setCapacity(request.capacity());
        room.setStatusCode(request.statusCode());
        room.setFloor(request.floor() != null ? request.floor() : 1);
        room.setDescription(request.description());
    }

    private void applyRequest(Room room, UpdateRoomRequest request) {
        room.setRoomCode(request.roomCode());
        room.setName(request.name());
        room.setRoomType(resolveRoomType(request.roomTypeId()));
        room.setCapacity(request.capacity());
        room.setStatusCode(request.statusCode());
        room.setFloor(request.floor() != null ? request.floor() : 1);
        room.setDescription(request.description());
    }

    private RoomType resolveRoomType(UUID roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_004_TYPE_NOT_FOUND));
    }

    private void ensureUniqueCode(String roomCode, UUID excludeId) {
        boolean exists = excludeId == null
                ? roomRepository.existsByRoomCode(roomCode)
                : roomRepository.existsByRoomCodeAndIdNot(roomCode, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_ROOM_002_CODE_EXISTS);
        }
    }

    private void ensureUniqueName(String name, UUID excludeId) {
        boolean exists = excludeId == null
                ? roomRepository.existsByName(name)
                : roomRepository.existsByNameAndIdNot(name, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_ROOM_003_NAME_EXISTS);
        }
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                room.getRoomType().getId(),
                room.getRoomType().getName(),
                room.getFloor(),
                room.getCapacity(),
                room.getStatusCode(),
                statusLabel(room.getStatusCode()),
                room.getDescription()
        );
    }

    static String statusLabel(RoomStatus status) {
        return switch (status) {
            case AVAILABLE -> "Trống";
            case OCCUPIED -> "Đang sử dụng";
            case MAINTENANCE -> "Bảo trì";
            case INACTIVE -> "Ngừng áp dụng";
        };
    }
}
