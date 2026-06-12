package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomAvailabilityChecker {
    private final ExamRoomRepository examRoomRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final AppointmentOverlapChecker overlapChecker;

    public ExamRoom requireRoomAvailable(OffsetDateTime startAt, OffsetDateTime endAt) {
        return examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().stream()
                .filter(room -> !overlapChecker.hasRoomOverlap(room.getId(), startAt, endAt))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL));
    }

    public void requireGroomingSlotAvailable(OffsetDateTime startAt, OffsetDateTime endAt) {
        int stations = groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc().size();
        if (stations == 0) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
        long booked = overlapChecker.countGroomingOverlap(startAt, endAt);
        if (booked >= stations) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
    }

    public long countFreeRooms(OffsetDateTime startAt, OffsetDateTime endAt) {
        return examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().stream()
                .filter(room -> !overlapChecker.hasRoomOverlap(room.getId(), startAt, endAt))
                .count();
    }
    
    public int getTotalActiveRooms() {
        return examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().size();
    }
}
