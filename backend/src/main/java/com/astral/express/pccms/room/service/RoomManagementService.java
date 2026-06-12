package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import com.astral.express.pccms.room.service.RoomManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomManagementService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
public PageResponse<RoomResponse> searchRooms(UUID roomTypeId, RoomStatus statusCode, Pageable pageable) {
        Page<Room> page = roomRepository.searchRooms(
                roomTypeId,
                statusCode == null ? null : statusCode.name(),
                pageable
        );
        return PageResponse.of(page.map(this::toResponse));
    }
public RoomResponse getRoom(UUID roomId) {
        return toResponse(findRoom(roomId));
    }
@Transactional
    public RoomResponse createRoom(RoomRequest request) {
        validateCapacity(request.capacity());
        if (request.roomCode() != null && !request.roomCode().isBlank() && roomRepository.existsByRoomCodeIgnoreCase(request.roomCode())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        Room room = new Room();
        String generatedRoomCode = request.roomCode();
        if (generatedRoomCode == null || generatedRoomCode.isBlank()) {
            generatedRoomCode = "ROOM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        room.setRoomCode(generatedRoomCode);
        applyRequest(room, request);
        return toResponse(roomRepository.save(room));
    }
@Transactional
    public RoomResponse updateRoom(UUID roomId, RoomRequest request) {
        validateCapacity(request.capacity());
        Room room = findRoom(roomId);
        if (request.roomCode() != null && !request.roomCode().isBlank() && roomRepository.existsByRoomCodeIgnoreCaseAndIdNot(request.roomCode(), roomId)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        applyRequest(room, request);
        return toResponse(roomRepository.save(room));
    }
@Transactional
    public RoomResponse updateRoomStatus(UUID roomId, RoomStatus statusCode) {
        Room room = findRoom(roomId);
        room.setStatusCode(statusCode);
        return toResponse(roomRepository.save(room));
    }
@Transactional
    public RoomResponse deactivateRoom(UUID roomId) {
        Room room = findRoom(roomId);
        if (roomRepository.countRoomAllocations(roomId) > 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        room.setStatusCode(RoomStatus.INACTIVE);
        return toResponse(roomRepository.save(room));
    }

    private void applyRequest(Room room, RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        if (!Boolean.TRUE.equals(roomType.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        if (request.roomCode() != null && !request.roomCode().isBlank()) {
            room.setRoomCode(request.roomCode());
        }
        room.setName(request.name());
        room.setRoomType(roomType);
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        room.setStatusCode(request.statusCode());
        room.setDescription(request.description());
    }

    private Room findRoom(UUID roomId) {
        return roomRepository.findWithRoomTypeById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private RoomResponse toResponse(Room room) {
        RoomType roomType = room.getRoomType();
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                roomType == null ? null : roomType.getId(),
                roomType == null ? null : roomType.getName(),
                room.getFloor(),
                room.getCapacity(),
                room.getStatusCode(),
                room.getDescription()
        );
    }

}


