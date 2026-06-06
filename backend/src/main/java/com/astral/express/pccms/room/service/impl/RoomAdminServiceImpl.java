package com.astral.express.pccms.room.service.impl;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAdminServiceImpl implements RoomAdminService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(RoomTypeRequest request) {
        validateRoomTypeRequest(request);
        RoomType roomType = roomMapper.toRoomType(request);
        return roomMapper.toRoomTypeResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(UUID id, RoomTypeRequest request) {
        validateRoomTypeRequest(request);
        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        roomMapper.updateRoomType(request, roomType);
        return roomMapper.toRoomTypeResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public void deactivateRoomType(UUID id) {
        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        roomType.setIsActive(false);
        roomTypeRepository.save(roomType);
    }

    @Override
    public List<RoomTypeResponse> listActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(roomMapper::toRoomTypeResponse)
                .toList();
    }

    @Override
    public PageResponse<RoomResponse> listRooms(Pageable pageable) {
        return PageResponse.of(roomRepository.findAll(pageable).map(roomMapper::toRoomResponse));
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        validateRoomRequest(request);
        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        Room room = roomMapper.toRoom(request);
        room.setRoomType(roomType);
        return roomMapper.toRoomResponse(roomRepository.save(room));
    }

    @Override
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

    @Override
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
        if (request.baseDailyPriceVnd() == null || request.baseDailyPriceVnd().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void validateRoomRequest(RoomRequest request) {
        if (request.capacity() == null || request.capacity() < 1 || request.floor() == null || request.floor() < 1) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }
}
