package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.boarding.support.BoardingPeriodLabels;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardingTrackingServiceImpl implements BoardingTrackingService {

    private final CareLogRepository careLogRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BoardingStayResponse> listActiveStays(UUID ownerId) {
        return careLogRepository.findActiveStaysByOwner(ownerId).stream()
                .map(row -> new BoardingStayResponse(
                        (UUID) row[0],
                        (String) row[1],
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : null
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareLogResponse> listCareLogs(UUID ownerId, UUID petId) {
        List<CareLog> logs = careLogRepository.findActiveStayLogsByOwner(ownerId, petId);
        if (logs.isEmpty()) {
            return List.of();
        }

        List<UUID> petIds = logs.stream().map(log -> log.getPet().getId()).distinct().toList();
        Map<UUID, Pets> petsById = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(Pets::getId, Function.identity()));

        return logs.stream()
                .map(log -> {
                    Pets pet = petsById.get(log.getPet().getId());
                    String petName = pet != null ? pet.getName() : "";
                    return new CareLogResponse(
                            log.getId(),
                            log.getSession().getId(),
                            log.getLogDate(),
                            log.getPeriodCode(),
                            log.getFeedingStatus(),
                            log.getHygieneStatus(),
                            log.getHealthNote(),
                            log.getStaffNote(),
                            log.getStaff().getId(),
                            log.getStaff().getFullName(),
                            log.getCreatedAt(),
                            Collections.emptyList()
                    );
                })
                .toList();
    }
}
