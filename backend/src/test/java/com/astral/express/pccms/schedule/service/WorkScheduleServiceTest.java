package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @Test
    void should_CreateSchedule_Success() {
        // GIVEN
        WorkScheduleRequest request = new WorkScheduleRequest(
                UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), null, null, UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note"
        );
        Users staff = staff("test@gmail.com", "Name", role("VETERINARIAN"));
        Shift shift = shift("MORNING", "Morning");
        Roles role = role("VETERINARIAN");

        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(staff));
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(shift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(role));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(request.staffId(), request.workDate(), request.shiftId()))
                .thenReturn(false);

        WorkSchedule saved = new WorkSchedule();
        saved.setId(UUID.randomUUID());
        saved.setStaff(staff);
        saved.setShift(shift);
        saved.setRole(role);
        when(workScheduleRepository.save(any(WorkSchedule.class))).thenReturn(saved);

        // WHEN
        com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse response = service.createSchedule(request);

        // THEN
        assertThat(response).isNotNull();
        verify(workScheduleRepository).save(any(WorkSchedule.class));
    }

    @Test
    void should_UpdateSchedule_Success() {
        // GIVEN
        UUID scheduleId = UUID.randomUUID();
        WorkScheduleRequest request = new WorkScheduleRequest(
                UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), null, null, UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note"
        );
        Users staff = staff("test@gmail.com", "Name", role("VETERINARIAN"));
        Shift shift = shift("MORNING", "Morning");
        Roles role = role("VETERINARIAN");

        WorkSchedule existing = new WorkSchedule();
        existing.setId(scheduleId);

        when(workScheduleRepository.findById(scheduleId)).thenReturn(java.util.Optional.of(existing));
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(staff));
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(shift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(role));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftIdAndIdNot(request.staffId(), request.workDate(), request.shiftId(), scheduleId))
                .thenReturn(false);
        when(workScheduleRepository.save(any(WorkSchedule.class))).thenReturn(existing);

        // WHEN
        com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse response = service.updateSchedule(scheduleId, request);

        // THEN
        assertThat(response).isNotNull();
        verify(workScheduleRepository).save(any(WorkSchedule.class));
    }

    @Test
    void should_CancelSchedule_Success() {
        // GIVEN
        UUID scheduleId = UUID.randomUUID();
        WorkSchedule existing = new WorkSchedule();
        existing.setId(scheduleId);

        when(workScheduleRepository.findById(scheduleId)).thenReturn(java.util.Optional.of(existing));
        when(workScheduleRepository.save(any(WorkSchedule.class))).thenReturn(existing);

        // WHEN
        com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse response = service.cancelSchedule(scheduleId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(existing.getStatusCode()).isEqualTo(ScheduleStatus.CANCELLED);
    }


    @Test
    void should_SkipSchedule_when_ApplyWeeklyPlanConflict() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(sourceWeekStart);
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        schedule.setStaff(staff);
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        schedule.setShift(shift);
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of(schedule));
        // Simulate conflict
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(true);
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(request);
        assertThat(res).isNotNull();
        assertThat(res.skippedCount()).isEqualTo(1); // One from source list
        assertThat(res.createdCount()).isEqualTo(0);
    }

    @Test
    void should_FilterSourceSchedules_when_RoleAndShiftDoNotMatch() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        
        UUID roleId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(roleId), List.of(shiftId));
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(sourceWeekStart);
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        schedule.setStaff(staff);
        
        Roles diffRole = new Roles();
        diffRole.setId(UUID.randomUUID()); // different role
        schedule.setRole(diffRole);
        
        Shift diffShift = new Shift();
        diffShift.setId(UUID.randomUUID()); // different shift
        schedule.setShift(diffShift);
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of(schedule));
        
        WeeklySchedulePlanResponse res = service.previewWeeklyPlan(request);
        assertThat(res).isNotNull();
        // Since the source doesn't match the filter, it's ignored.
        // It might try to generate if any staff fits
        assertThat(res.items()).isEmpty(); // Assuming no staff from userRepository either
    }

    @Test
    void should_GenerateSchedulesWithConflict_when_ApplyWeeklyPlan() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of());
                
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Generated Staff");
        Roles role = new Roles();
        role.setId(UUID.randomUUID());
        staff.setRole(role);
        
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCode("MORNING");
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staff));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        
        // Always conflict
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(true);
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(request);
        
        assertThat(res).isNotNull();
        assertThat(res.skippedCount()).isEqualTo(7); // 7 days in week
        assertThat(res.createdCount()).isEqualTo(0);
    }



    @Test
    void should_GetWorkSchedules_WithVariousFilters() {
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(UUID.randomUUID());
        Users mockStaff = new Users(); mockStaff.setId(UUID.randomUUID()); schedule.setStaff(mockStaff);
        schedule.setWorkDate(LocalDate.now());
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Staff");
        schedule.setStaff(staff);
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setName("Shift");
        schedule.setShift(shift);
        
        org.springframework.data.domain.Page<WorkSchedule> page = new org.springframework.data.domain.PageImpl<>(List.of(schedule));
        
        // Use any() for Pageable
        when(workScheduleRepository.findByWorkDateBetween(any(), any(), eq(pageable))).thenReturn(page);
        
        com.astral.express.pccms.common.dto.PageResponse<com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse> res = service.searchSchedules(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                pageable
        );
        
        assertThat(res).isNotNull();
        assertThat(res.data().content()).hasSize(1);
    }

    @Test
    void should_CreateWeeklySchedules_EmptyStaffOrShifts() {
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                List.of(),
                List.of()
        );
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of()); // Empty staff
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(request);
        
        assertThat(res.createdCount()).isEqualTo(0);
        assertThat(res.skippedCount()).isEqualTo(0);
    }

    @Test
    void searchSchedules_shouldThrow_whenDatesInvalid() {
        assertThatThrownBy(() -> service.searchSchedules(LocalDate.now(), LocalDate.now().minusDays(1), org.springframework.data.domain.PageRequest.of(0, 10)))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenCapacityInvalid() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, ScheduleStatus.ASSIGNED, "Note");
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenShiftInactive() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(new Users()));
        Shift inactiveShift = new Shift();
        inactiveShift.setIsActive(false);
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(inactiveShift));
        
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void updateSchedule_shouldThrow_whenNotFound() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(workScheduleRepository.findById(any())).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.updateSchedule(UUID.randomUUID(), req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenDuplicate() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(new Users()));
        Shift activeShift = new Shift(); activeShift.setIsActive(true);
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(activeShift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(new Roles()));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(true);
        
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void updateSchedule_shouldThrow_whenDuplicate() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(workScheduleRepository.findById(any())).thenReturn(java.util.Optional.of(new WorkSchedule()));
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(new Users()));
        Shift activeShift = new Shift(); activeShift.setIsActive(true);
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(activeShift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(new Roles()));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftIdAndIdNot(any(), any(), any(), any())).thenReturn(true);
        
        assertThatThrownBy(() -> service.updateSchedule(UUID.randomUUID(), req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }
    
    @Test
    void createSchedule_shouldThrow_whenExamRoomNotFound() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(new Users()));
        Shift activeShift = new Shift(); activeShift.setIsActive(true);
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(activeShift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(new Roles()));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(examRoomRepository.findById(any())).thenReturn(java.util.Optional.empty());
        
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenStationNotFound() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), 1, ScheduleStatus.ASSIGNED, "Note");
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(new Users()));
        Shift activeShift = new Shift(); activeShift.setIsActive(true);
        when(shiftRepository.findById(any())).thenReturn(java.util.Optional.of(activeShift));
        when(roleRepository.findById(any())).thenReturn(java.util.Optional.of(new Roles()));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(groomingStationRepository.findById(any())).thenReturn(java.util.Optional.empty());
        
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void previewWeeklyPlan_shouldThrow_whenDatesInvalid() {
        WeeklySchedulePlanRequest req = new WeeklySchedulePlanRequest(LocalDate.now(), LocalDate.now(), List.of(), List.of());
        assertThatThrownBy(() -> service.previewWeeklyPlan(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }
    
    @Test
    void applyWeeklyPlan_shouldSave_whenSourceTemplateExists() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        WeeklySchedulePlanRequest req = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, null, null);
        
        WorkSchedule source = new WorkSchedule();
        source.setId(UUID.randomUUID());
        source.setWorkDate(sourceWeekStart);
        Users staff = new Users(); staff.setId(UUID.randomUUID()); source.setStaff(staff);
        Shift shift = new Shift(); shift.setId(UUID.randomUUID()); source.setShift(shift);
        source.setRole(new Roles());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any())).thenReturn(List.of(source));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(workScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(req);
        assertThat(res.createdCount()).isEqualTo(1);
    }



    @Test
    void searchSchedules_shouldThrow_whenDatesInvalid_inverted() {
        assertThatThrownBy(() -> service.searchSchedules(LocalDate.now(), LocalDate.now().minusDays(1), org.springframework.data.domain.PageRequest.of(0, 10)))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
        assertThatThrownBy(() -> service.searchSchedules(null, LocalDate.now().minusDays(1), org.springframework.data.domain.PageRequest.of(0, 10)))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenCapacityNegative() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), -1, ScheduleStatus.ASSIGNED, "Note");
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void previewWeeklyPlan_shouldThrow_whenDatesSame() {
        LocalDate date = LocalDate.now();
        WeeklySchedulePlanRequest req = new WeeklySchedulePlanRequest(date, date, List.of(), List.of());
        assertThatThrownBy(() -> service.previewWeeklyPlan(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void applyWeeklyPlan_shouldGenerateSchedules_whenPersistTrue() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of());
                
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Generated Staff");
        Roles role = new Roles();
        role.setId(UUID.randomUUID());
        staff.setRole(role);
        
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCode("MORNING");
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staff));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        
        // No conflict, will save
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(workScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(request);
        
        assertThat(res).isNotNull();
        assertThat(res.createdCount()).isEqualTo(7);
        assertThat(res.skippedCount()).isEqualTo(0);
    }
    
    @Test
    void previewWeeklyPlan_withRoleAndShiftFilters() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        
        UUID roleId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(roleId), List.of(shiftId));
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(sourceWeekStart);
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        schedule.setStaff(staff);
        
        Roles matchingRole = new Roles();
        matchingRole.setId(roleId);
        schedule.setRole(matchingRole);
        
        Shift matchingShift = new Shift();
        matchingShift.setId(shiftId);
        schedule.setShift(matchingShift);
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of(schedule));
                
        Users staffOption = new Users();
        staffOption.setId(UUID.randomUUID());
        staffOption.setRole(matchingRole);
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staffOption));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(matchingShift));
        
        WeeklySchedulePlanResponse res = service.previewWeeklyPlan(request);
        assertThat(res).isNotNull();
    }
    
    @Test
    void previewWeeklyPlan_withStaffNulls() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(sourceWeekStart);
        Users sourceStaff = new Users();
        sourceStaff.setId(UUID.randomUUID());
        schedule.setStaff(sourceStaff);
        Shift sourceShift = new Shift();
        sourceShift.setId(UUID.randomUUID());
        schedule.setShift(sourceShift);
        // Null role
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of(schedule));
                
        Users staffOption1 = new Users();
        staffOption1.setId(UUID.randomUUID());
        // Null role
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staffOption1));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of());
        
        WeeklySchedulePlanResponse res = service.previewWeeklyPlan(request);
        assertThat(res).isNotNull();
    }



    @Test
    void searchSchedules_shouldThrow_whenDatesNull() {
        assertThatThrownBy(() -> service.searchSchedules(LocalDate.now(), null, org.springframework.data.domain.PageRequest.of(0, 10)))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void createSchedule_shouldThrow_whenCapacityNull() {
        WorkScheduleRequest req = new WorkScheduleRequest(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, ScheduleStatus.ASSIGNED, "Note");
        assertThatThrownBy(() -> service.createSchedule(req))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void previewWeeklyPlan_shouldThrow_whenDatesNull() {
        WeeklySchedulePlanRequest req1 = new WeeklySchedulePlanRequest(null, LocalDate.now(), List.of(), List.of());
        assertThatThrownBy(() -> service.previewWeeklyPlan(req1))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
            
        WeeklySchedulePlanRequest req2 = new WeeklySchedulePlanRequest(LocalDate.now(), null, List.of(), List.of());
        assertThatThrownBy(() -> service.previewWeeklyPlan(req2))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void previewWeeklyPlan_withEmptyShifts() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of());
                
        Users staffOption1 = new Users();
        staffOption1.setId(UUID.randomUUID());
        staffOption1.setRole(new Roles());
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staffOption1));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of()); // Empty shifts
        
        WeeklySchedulePlanResponse res = service.previewWeeklyPlan(request);
        assertThat(res).isNotNull();
        assertThat(res.createdCount()).isEqualTo(0);
    }

    @Test
    void previewWeeklyPlan_withToGeneratedPlanItem_StaffAndRole() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of());
                
        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("No Role Staff");
        Roles role = new Roles();
        role.setId(UUID.randomUUID());
        staff.setRole(role);
        
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCode("MORNING");
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(staff));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        
        WeeklySchedulePlanResponse res = service.previewWeeklyPlan(request);
        assertThat(res).isNotNull();
        assertThat(res.items()).hasSize(7);
    }

    @Test
    void applyWeeklyPlan_shouldAssignRoomsAndStations_whenGenerated() {
        LocalDate sourceWeekStart = LocalDate.now();
        LocalDate targetWeekStart = sourceWeekStart.plusWeeks(1);
        WeeklySchedulePlanRequest request = new WeeklySchedulePlanRequest(sourceWeekStart, targetWeekStart, List.of(), List.of());
        
        when(workScheduleRepository.findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(any(), any(), any()))
                .thenReturn(List.of());
                
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        vet.setFullName("Generated Vet");
        Roles vetRole = new Roles();
        vetRole.setCode("VETERINARIAN");
        vetRole.setId(UUID.randomUUID());
        vet.setRole(vetRole);

        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Generated Staff");
        Roles staffRole = new Roles();
        staffRole.setCode("STAFF");
        staffRole.setId(UUID.randomUUID());
        staff.setRole(staffRole);
        
        Shift shift = new Shift();
        shift.setId(UUID.randomUUID());
        shift.setCode("MORNING");
        
        when(userRepository.findScheduleStaffOptions(any(), any())).thenReturn(List.of(vet, staff));
        when(shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(shift));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(workScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        com.astral.express.pccms.appointment.entity.ExamRoom room = new com.astral.express.pccms.appointment.entity.ExamRoom();
        room.setId(UUID.randomUUID());
        room.setRoomCode("R1");
        
        com.astral.express.pccms.grooming.entity.GroomingStation station = new com.astral.express.pccms.grooming.entity.GroomingStation();
        station.setId(UUID.randomUUID());
        station.setStationCode("S1");

        when(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).thenReturn(List.of(room));
        when(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()).thenReturn(List.of(station));
        
        WeeklySchedulePlanResponse res = service.applyWeeklyPlan(request);
        
        assertThat(res).isNotNull();
        assertThat(res.createdCount()).isEqualTo(14);
        
        org.mockito.ArgumentCaptor<WorkSchedule> captor = org.mockito.ArgumentCaptor.forClass(WorkSchedule.class);
        verify(workScheduleRepository, org.mockito.Mockito.times(14)).save(captor.capture());
        
        List<WorkSchedule> savedSchedules = captor.getAllValues();
        
        assertThat(savedSchedules.stream().filter(s -> "VETERINARIAN".equals(s.getRole().getCode())))
                .allMatch(s -> s.getExamRoom() != null && s.getExamRoom().getRoomCode().equals("R1") && s.getStation() == null);
                
        assertThat(savedSchedules.stream().filter(s -> "STAFF".equals(s.getRole().getCode())))
                .allMatch(s -> s.getStation() != null && s.getStation().getStationCode().equals("S1") && s.getExamRoom() == null);
    }

}
