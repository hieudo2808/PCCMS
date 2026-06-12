package com.astral.express.pccms.room.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.dto.request.RoomRequest;
import com.astral.express.pccms.room.dto.request.RoomStatusUpdateRequest;
import com.astral.express.pccms.room.dto.request.RoomTypeRequest;
import com.astral.express.pccms.room.dto.response.RoomResponse;
import com.astral.express.pccms.room.dto.response.RoomTypeResponse;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.mapper.RoomMapper;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import com.astral.express.pccms.room.service.RoomAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAdminService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
public List<RoomTypeResponse> listRoomTypes(boolean activeOnly) {
        List<RoomType> roomTypes = activeOnly
                ? roomTypeRepository.findByIsActiveTrueOrderByNameAsc()
                : roomTypeRepository.findAllByOrderByNameAsc();
        return roomTypes.stream()
                .map(roomMapper::toRoomTypeResponse)
                .toList();
    }
public RoomTypeResponse getRoomType(UUID id) {
        return roomMapper.toRoomTypeResponse(findRoomType(id));
    }
@Transactional
    public RoomTypeResponse createRoomType(RoomTypeRequest request) {
        validateRoomTypeRequest(request);
        String code = resolveRoomTypeCode(request.code(), request.name(), null);
        if (roomTypeRepository.existsByCodeAndIsActiveTrue(code)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
        }
        RoomType roomType = roomMapper.toRoomType(request);
        roomType.setCode(code);
        roomType.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        return roomMapper.toRoomTypeResponse(roomTypeRepository.save(roomType));
    }
@Transactional
    public RoomTypeResponse updateRoomType(UUID id, RoomTypeRequest request) {
        validateRoomTypeRequest(request);
        RoomType roomType = findRoomType(id);
        String code = resolveRoomTypeCode(request.code(), request.name(), id);
        if (roomTypeRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
        }
        Boolean previousActive = roomType.getIsActive();
        roomMapper.updateRoomType(request, roomType);
        roomType.setCode(code);
        if (request.isActive() == null) {
            roomType.setIsActive(previousActive);
        }
        return roomMapper.toRoomTypeResponse(roomTypeRepository.save(roomType));
    }
@Transactional
    public RoomTypeResponse updateRoomTypeActive(UUID id, Boolean isActive) {
        RoomType roomType = findRoomType(id);
        roomType.setIsActive(Boolean.TRUE.equals(isActive));
        return roomMapper.toRoomTypeResponse(roomTypeRepository.save(roomType));
    }
@Transactional
    public void deactivateRoomType(UUID id) {
        RoomType roomType = findRoomType(id);
        if (roomRepository.existsByRoomTypeId(id)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_007_TYPE_IN_USE);
        }
        roomType.setIsActive(false);
        roomTypeRepository.save(roomType);
    }
public List<RoomTypeResponse> listActiveRoomTypes() {
        return listRoomTypes(true);
    }
public PageResponse<RoomResponse> listRooms(Pageable pageable) {
        return PageResponse.of(roomRepository.findAll(pageable).map(roomMapper::toRoomResponse));
    }
@Transactional
    public RoomResponse createRoom(RoomRequest request) {
        validateRoomRequest(request);
        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        Room room = roomMapper.toRoom(request);
        room.setRoomType(roomType);
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }
@Transactional
    public RoomResponse updateRoom(UUID id, RoomRequest request) {
        validateRoomRequest(request);
        Room room = roomRepository.findWithRoomTypeById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND));
        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        roomMapper.updateRoom(request, room);
        room.setRoomType(roomType);
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }
@Transactional
    public RoomResponse updateRoomStatus(UUID id, RoomStatusUpdateRequest request) {
        Room room = roomRepository.findWithRoomTypeById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND));
        room.setStatusCode(request.statusCode());
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }

    private void validateRoomTypeRequest(RoomTypeRequest request) {
        if (request.defaultCapacity() == null || request.defaultCapacity() < 1) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (request.baseDailyPriceVnd() == null || request.baseDailyPriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void validateRoomRequest(RoomRequest request) {
        if (request.capacity() == null || request.capacity() < 1 || request.floor() == null || request.floor() < 1) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private RoomType findRoomType(UUID id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
    }

    private String resolveRoomTypeCode(String requestedCode, String name, UUID existingId) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return normalizeRoomTypeCode(requestedCode);
        }
        if (existingId != null) {
            return findRoomType(existingId).getCode();
        }

        String prefix = normalizeRoomTypeCode(name).replaceAll("[^A-Z0-9]+", "");
        if (prefix.length() > 10) {
            prefix = prefix.substring(0, 10);
        }
        if (prefix.isBlank()) {
            prefix = "ROOMTYPE";
        }
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String candidate = "RT-" + prefix + "-" + String.format("%04d", sequence);
            if (!roomTypeRepository.existsByCodeAndIsActiveTrue(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
    }

    private String normalizeRoomTypeCode(String value) {
        return value.trim().toUpperCase();
    }
}


