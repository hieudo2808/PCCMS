package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceTest {
    @Mock
    private WorkScheduleRepository workScheduleRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ExamRoomRepository examRoomRepository;
    @Mock
    private GroomingStationRepository groomingStationRepository;

    @InjectMocks
    private WorkScheduleService service;

    @Test
    void previewWeeklyPlanGeneratesSchedulesWhenSourceWeekHasNoTemplate() {
        LocalDate sourceWeekStart = LocalDate.of(2026, 6, 1);
        LocalDate targetWeekStart = LocalDate.of(2026, 6, 8);
        Roles veterinarianRole = role("VETERINARIAN");
        Users veterinarian = staff("vet-1@pccms.local", "Bac si thu y 1", veterinarianRole);
        Shift morningShift = shift("MORNING", "Ca sang");

        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(
                sourceWeekStart, sourceWeekStart.plusDays(6), ScheduleStatus.ASSIGNED))
                .thenReturn(List.of());
        when(userRepository.findScheduleStaffOptions(eq(UserStatus.ACTIVE), anyList()))
                .thenReturn(List.of(veterinarian));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc())
                .thenReturn(List.of(morningShift));

        WeeklySchedulePlanResponse response = service.previewWeeklyPlan(new WeeklySchedulePlanRequest(
                sourceWeekStart,
                targetWeekStart,
                List.of(veterinarianRole.getId()),
                List.of(morningShift.getId())
        ));

        assertThat(response.createdCount()).isZero();
        assertThat(response.skippedCount()).isZero();
        assertThat(response.items()).hasSize(7);
        assertThat(response.items().getFirst().sourceScheduleId()).isNull();
        assertThat(response.items().getFirst().staffId()).isEqualTo(veterinarian.getId());
        assertThat(response.items().getFirst().targetDate()).isEqualTo(targetWeekStart);
        assertThat(response.items().getFirst().shiftId()).isEqualTo(morningShift.getId());
        assertThat(response.items()).allMatch(item -> !item.conflict());
        verify(workScheduleRepository, never()).save(any());
    }

    private static Roles role(String code) {
        Roles role = new Roles();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setName(code);
        role.setIsActive(true);
        return role;
    }

    private static Users staff(String email, String fullName, Roles role) {
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setStatusCode(UserStatus.ACTIVE);
        return user;
    }

    private static Shift shift(String code, String name) {
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCode(code);
        shift.setName(name);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));
        shift.setIsActive(true);
        return shift;
    }
}


