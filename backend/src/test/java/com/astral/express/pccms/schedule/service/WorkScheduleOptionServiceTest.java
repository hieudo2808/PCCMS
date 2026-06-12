package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.schedule.dto.response.ExamRoomOptionResponse;
import com.astral.express.pccms.schedule.dto.response.GroomingStationOptionResponse;
import com.astral.express.pccms.schedule.dto.response.RoleOptionResponse;
import com.astral.express.pccms.schedule.dto.response.ShiftOptionResponse;
import com.astral.express.pccms.schedule.dto.response.StaffOptionResponse;
import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WorkScheduleOptionServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ExamRoomRepository examRoomRepository;

    @Mock
    private GroomingStationRepository groomingStationRepository;

    @InjectMocks
    private WorkScheduleOptionService workScheduleOptionService;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvFileSource(resources = "/testcases/work-schedule-option-testcases.csv", numLinesToSkip = 1)
    void should_ProcessOptions(String ruleId, String caseId, String action, boolean hasData) {
        if ("STAFF_OPTIONS".equals(action)) {
            if (hasData) {
                Roles role = role("1", "STAFF", "Nhan vien");
                Users user = new Users();
                user.setId(id("10"));
                user.setFullName("Staff One");
                user.setRole(role);

                given(userRepository.findScheduleStaffOptions(eq(UserStatus.ACTIVE), org.mockito.ArgumentMatchers.<String>anyList()))
                        .willReturn(List.of(user));

                List<StaffOptionResponse> response = workScheduleOptionService.getStaffOptions();

                assertThat(response).hasSize(1);
                assertThat(response.getFirst().id()).isEqualTo(id("10"));
                assertThat(response.getFirst().fullName()).isEqualTo("Staff One");
                assertThat(response.getFirst().roleCode()).isEqualTo("STAFF");
                assertThat(response.getFirst().roleName()).isEqualTo("Nhan vien");
            } else {
                given(userRepository.findScheduleStaffOptions(eq(UserStatus.ACTIVE), org.mockito.ArgumentMatchers.<String>anyList()))
                        .willReturn(List.of());

                List<StaffOptionResponse> response = workScheduleOptionService.getStaffOptions();

                assertThat(response).isEmpty();
            }
        } else if ("SHIFT_OPTIONS".equals(action)) {
            Shift shift = new Shift();
            shift.setId(id("20"));
            shift.setCode("MORNING");
            shift.setName("Morning");
            shift.setStartTime(LocalTime.of(8, 0));
            shift.setEndTime(LocalTime.of(12, 0));

            given(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).willReturn(List.of(shift));

            List<ShiftOptionResponse> response = workScheduleOptionService.getShiftOptions();

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(id("20"));
            assertThat(response.getFirst().shiftCode()).isEqualTo("MORNING");
            assertThat(response.getFirst().startTime()).isEqualTo(LocalTime.of(8, 0));
        } else if ("ROLE_OPTIONS".equals(action)) {
            Roles role = role("30", "VETERINARIAN", "Bac si");
            given(roleRepository.findByIsActiveTrueOrderByCodeAsc()).willReturn(List.of(role));

            List<RoleOptionResponse> response = workScheduleOptionService.getRoleOptions();

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(id("30"));
            assertThat(response.getFirst().code()).isEqualTo("VETERINARIAN");
        } else if ("EXAM_ROOM_OPTIONS".equals(action)) {
            ExamRoom room = new ExamRoom();
            room.setId(id("40"));
            room.setRoomCode("EX01");
            room.setName("Exam room 1");

            given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(room));

            List<ExamRoomOptionResponse> response = workScheduleOptionService.getExamRoomOptions();

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(id("40"));
            assertThat(response.getFirst().roomCode()).isEqualTo("EX01");
        } else if ("GROOMING_STATION_OPTIONS".equals(action)) {
            GroomingStation station = new GroomingStation();
            station.setId(id("50"));
            station.setStationCode("GR01");
            station.setName("Station 1");

            given(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()).willReturn(List.of(station));

            List<GroomingStationOptionResponse> response = workScheduleOptionService.getGroomingStationOptions();

            assertThat(response).hasSize(1);
            assertThat(response.getFirst().id()).isEqualTo(id("50"));
            assertThat(response.getFirst().stationCode()).isEqualTo("GR01");
        }
    }

    private Roles role(String id, String code, String name) {
        Roles role = new Roles();
        role.setId(id(id));
        role.setCode(code);
        role.setName(name);
        role.setIsActive(true);
        return role;
    }

    private UUID id(String value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", Long.parseLong(value)));
    }
}


