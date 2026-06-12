package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.schedule.dto.response.ExamRoomOptionResponse;
import com.astral.express.pccms.schedule.dto.response.GroomingStationOptionResponse;
import com.astral.express.pccms.schedule.dto.response.RoleOptionResponse;
import com.astral.express.pccms.schedule.dto.response.ShiftOptionResponse;
import com.astral.express.pccms.schedule.dto.response.StaffOptionResponse;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.schedule.service.WorkScheduleOptionService;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleOptionService {
    private static final List<String> SCHEDULABLE_ROLE_CODES = List.of("STAFF", "VETERINARIAN", "RECEPTIONIST");

    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final RoleRepository roleRepository;
    private final ExamRoomRepository examRoomRepository;
    private final GroomingStationRepository groomingStationRepository;
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('STAFF') or hasRole('VETERINARIAN')")
    public List<StaffOptionResponse> getStaffOptions() {
        return userRepository.findScheduleStaffOptions(UserStatus.ACTIVE, SCHEDULABLE_ROLE_CODES)
                .stream()
                .map(user -> {
                    Roles role = user.getRole();
                    return new StaffOptionResponse(
                            user.getId(),
                            user.getFullName(),
                            role == null ? null : role.getCode(),
                            role == null ? null : role.getName());
                })
                .toList();
    }
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public List<ShiftOptionResponse> getShiftOptions() {
        return shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()
                .stream()
                .map(shift -> new ShiftOptionResponse(
                        shift.getId(),
                        shift.getCode(),
                        shift.getName(),
                        shift.getStartTime(),
                        shift.getEndTime()))
                .toList();
    }
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public List<RoleOptionResponse> getRoleOptions() {
        return roleRepository.findByIsActiveTrueOrderByCodeAsc()
                .stream()
                .filter(role -> SCHEDULABLE_ROLE_CODES.contains(role.getCode()))
                .map(role -> new RoleOptionResponse(role.getId(), role.getCode(), role.getName()))
                .toList();
    }
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public List<ExamRoomOptionResponse> getExamRoomOptions() {
        return examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()
                .stream()
                .map(room -> new ExamRoomOptionResponse(room.getId(), room.getRoomCode(), room.getName()))
                .toList();
    }
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public List<GroomingStationOptionResponse> getGroomingStationOptions() {
        return groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()
                .stream()
                .map(station -> new GroomingStationOptionResponse(
                        station.getId(),
                        station.getStationCode(),
                        station.getName()))
                .toList();
    }
}



