package com.astral.express.pccms.catalog.service.impl;

import com.astral.express.pccms.appointment.entity.RoomType;
import com.astral.express.pccms.appointment.repository.RoomTypeRepository;
import com.astral.express.pccms.catalog.dto.request.CreateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateRoomTypeRequest;
import com.astral.express.pccms.catalog.dto.response.RoomTypeResponse;
import com.astral.express.pccms.catalog.repository.RoomRepository;
import com.astral.express.pccms.catalog.service.RoomTypeService;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public RoomTypeResponse create(CreateRoomTypeRequest request) {
        if (roomTypeRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
        }

        RoomType roomType = new RoomType();
        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public RoomTypeResponse update(UUID id, UpdateRoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_004_TYPE_NOT_FOUND));

        if (roomTypeRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_005_TYPE_CODE_EXISTS);
        }

        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeResponse getById(UUID id) {
        return roomTypeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_004_TYPE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listActive() {
        return roomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listAll() {
        return roomTypeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyRequest(RoomType roomType, CreateRoomTypeRequest request) {
        roomType.setCode(request.code());
        roomType.setName(request.name());
        roomType.setDefaultCapacity(request.defaultCapacity());
        roomType.setBaseDailyPriceVnd(request.baseDailyPriceVnd());
        roomType.setDescription(request.description());
        roomType.setIsActive(request.isActive());
    }

    private void applyRequest(RoomType roomType, UpdateRoomTypeRequest request) {
        roomType.setCode(request.code());
        roomType.setName(request.name());
        roomType.setDefaultCapacity(request.defaultCapacity());
        roomType.setBaseDailyPriceVnd(request.baseDailyPriceVnd());
        roomType.setDescription(request.description());
        roomType.setIsActive(request.isActive());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_004_TYPE_NOT_FOUND));
        if (roomRepository.existsByRoomTypeId(id)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_007_TYPE_IN_USE);
        }
        roomType.setIsActive(false);
        roomTypeRepository.save(roomType);
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getCode(),
                roomType.getName(),
                roomType.getDefaultCapacity(),
                roomType.getBaseDailyPriceVnd(),
                roomType.getDescription(),
                roomType.getIsActive()
        );
    }
}
