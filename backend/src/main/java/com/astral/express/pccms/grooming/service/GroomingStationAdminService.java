package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroomingStationAdminService {
    private final GroomingStationRepository groomingStationRepository;
    private final GroomingMapper groomingMapper;

    @Transactional(readOnly = true)
    public List<GroomingStationResponse> listStationsForAdmin() {
        return groomingStationRepository.findAll().stream()
                .map(groomingMapper::toStationResponse)
                .toList();
    }

    @Transactional
    public GroomingStationResponse createStation(GroomingStationRequest request) {
        if (groomingStationRepository.existsByStationCode(request.stationCode())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_008_STATION_CODE_EXISTS);
        }
        GroomingStation station = new GroomingStation();
        station.setStationCode(request.stationCode());
        station.setName(request.name());
        station.setIsActive(request.isActive());
        return groomingMapper.toStationResponse(groomingStationRepository.save(station));
    }

    @Transactional
    public GroomingStationResponse updateStation(UUID id, GroomingStationRequest request) {
        GroomingStation station = groomingStationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        if (groomingStationRepository.existsByStationCodeAndIdNot(request.stationCode(), id)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_008_STATION_CODE_EXISTS);
        }
        station.setStationCode(request.stationCode());
        station.setName(request.name());
        station.setIsActive(request.isActive());
        return groomingMapper.toStationResponse(groomingStationRepository.save(station));
    }

    @Transactional
    public void deactivateStation(UUID id) {
        GroomingStation station = groomingStationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        station.setIsActive(false);
        groomingStationRepository.save(station);
    }
}
