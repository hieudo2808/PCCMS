package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanItemResponse;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanResponse;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.schedule.service.WorkScheduleService;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleService {
    private static final int DAYS_IN_WEEK = 7;
    private static final List<String> SCHEDULABLE_ROLE_CODES = List.of("STAFF", "VETERINARIAN", "RECEPTIONIST");

    private final WorkScheduleRepository workScheduleRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ExamRoomRepository examRoomRepository;
    private final GroomingStationRepository groomingStationRepository;
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public PageResponse<WorkScheduleResponse> searchSchedules(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        validateDateRange(fromDate, toDate);
        Page<WorkSchedule> schedules = workScheduleRepository.findByWorkDateBetween(fromDate, toDate, pageable);
        return PageResponse.of(schedules.map(ScheduleMapperSupport::toWorkScheduleResponse));
    }
@Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse createSchedule(WorkScheduleRequest request) {
        validateCapacity(request.capacity());
        Users staff = findUser(request.staffId());
        Shift shift = findActiveShift(request.shiftId());
        Roles role = findRole(request.roleId());
        if (workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(
                request.staffId(), request.workDate(), request.shiftId())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        WorkSchedule schedule = new WorkSchedule();
        applyRequest(schedule, request, staff, shift, role);
        return ScheduleMapperSupport.toWorkScheduleResponse(workScheduleRepository.save(schedule));
    }
@Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse updateSchedule(UUID scheduleId, WorkScheduleRequest request) {
        validateCapacity(request.capacity());
        WorkSchedule schedule = findSchedule(scheduleId);
        Users staff = findUser(request.staffId());
        Shift shift = findActiveShift(request.shiftId());
        Roles role = findRole(request.roleId());
        if (workScheduleRepository.existsByStaffIdAndWorkDateAndShiftIdAndIdNot(
                request.staffId(), request.workDate(), request.shiftId(), scheduleId)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        applyRequest(schedule, request, staff, shift, role);
        return ScheduleMapperSupport.toWorkScheduleResponse(workScheduleRepository.save(schedule));
    }
@Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse cancelSchedule(UUID scheduleId) {
        WorkSchedule schedule = findSchedule(scheduleId);
        schedule.setStatusCode(ScheduleStatus.CANCELLED);
        return ScheduleMapperSupport.toWorkScheduleResponse(workScheduleRepository.save(schedule));
    }
@PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WeeklySchedulePlanResponse previewWeeklyPlan(WeeklySchedulePlanRequest request) {
        return buildWeeklyPlan(request, false);
    }
@Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WeeklySchedulePlanResponse applyWeeklyPlan(WeeklySchedulePlanRequest request) {
        return buildWeeklyPlan(request, true);
    }

    private WeeklySchedulePlanResponse buildWeeklyPlan(WeeklySchedulePlanRequest request, boolean persist) {
        validateWeeklyPlanRequest(request);

        LocalDate sourceEnd = request.sourceWeekStart().plusDays(6);
        List<WorkSchedule> sourceSchedules = workScheduleRepository
                .findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(
                        request.sourceWeekStart(), sourceEnd, ScheduleStatus.ASSIGNED)
                .stream()
                .filter(schedule -> matchesWeeklyPlanFilter(schedule, request))
                .toList();

        List<WeeklySchedulePlanItemResponse> items = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        Set<UUID> templatedStaffIds = sourceSchedules.stream()
                .filter(schedule -> schedule.getStaff() != null)
                .map(schedule -> schedule.getStaff().getId())
                .collect(Collectors.toSet());

        for (WorkSchedule source : sourceSchedules) {
            LocalDate targetDate = request.targetWeekStart()
                    .plusDays(ChronoUnit.DAYS.between(request.sourceWeekStart(), source.getWorkDate()));
            boolean conflict = workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(
                    source.getStaff().getId(), targetDate, source.getShift().getId());

            if (conflict) {
                skippedCount++;
                items.add(toWeeklyPlanItem(source, null, targetDate, true, "STAFF_SHIFT_ALREADY_EXISTS"));
                continue;
            }

            WorkSchedule created = null;
            if (persist) {
                created = cloneSchedule(source, targetDate);
                created = workScheduleRepository.save(created);
                createdCount++;
            }
            items.add(toWeeklyPlanItem(source, created, targetDate, false, null));
        }

        PlanCounts generatedCounts = appendGeneratedWeeklyPlanItems(request, persist, templatedStaffIds, items);
        createdCount += generatedCounts.createdCount();
        skippedCount += generatedCounts.skippedCount();

        return new WeeklySchedulePlanResponse(createdCount, skippedCount, items);
    }

    private PlanCounts appendGeneratedWeeklyPlanItems(
            WeeklySchedulePlanRequest request,
            boolean persist,
            Set<UUID> templatedStaffIds,
            List<WeeklySchedulePlanItemResponse> items) {
        Set<UUID> roleIds = toIdSet(request.roleIds());
        Set<UUID> shiftIds = toIdSet(request.shiftIds());
        List<Users> staffMembers = userRepository.findScheduleStaffOptions(UserStatus.ACTIVE, SCHEDULABLE_ROLE_CODES)
                .stream()
                .filter(user -> user.getRole() != null)
                .filter(user -> !templatedStaffIds.contains(user.getId()))
                .filter(user -> roleIds.isEmpty() || roleIds.contains(user.getRole().getId()))
                .sorted(Comparator.comparing(Users::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Shift> shifts = shiftRepository.findByIsActiveTrueOrderByStartTimeAsc()
                .stream()
                .filter(shift -> shiftIds.isEmpty() || shiftIds.contains(shift.getId()))
                .toList();

        if (staffMembers.isEmpty() || shifts.isEmpty()) {
            return new PlanCounts(0, 0);
        }

        int createdCount = 0;
        int skippedCount = 0;
        for (int dayOffset = 0; dayOffset < DAYS_IN_WEEK; dayOffset++) {
            LocalDate targetDate = request.targetWeekStart().plusDays(dayOffset);
            for (int staffIndex = 0; staffIndex < staffMembers.size(); staffIndex++) {
                Users staff = staffMembers.get(staffIndex);
                Shift shift = shifts.get((dayOffset + staffIndex) % shifts.size());
                boolean conflict = workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(
                        staff.getId(), targetDate, shift.getId());

                if (conflict) {
                    skippedCount++;
                    items.add(toGeneratedPlanItem(staff, shift, null, targetDate, true, "STAFF_SHIFT_ALREADY_EXISTS"));
                    continue;
                }

                WorkSchedule created = null;
                if (persist) {
                    created = createGeneratedSchedule(staff, shift, targetDate);
                    created = workScheduleRepository.save(created);
                    createdCount++;
                }
                items.add(toGeneratedPlanItem(staff, shift, created, targetDate, false, null));
            }
        }

        return new PlanCounts(createdCount, skippedCount);
    }

    private boolean matchesWeeklyPlanFilter(WorkSchedule schedule, WeeklySchedulePlanRequest request) {
        Set<UUID> roleIds = toIdSet(request.roleIds());
        Set<UUID> shiftIds = toIdSet(request.shiftIds());
        boolean matchesRole = roleIds.isEmpty()
                || (schedule.getRole() != null && roleIds.contains(schedule.getRole().getId()));
        boolean matchesShift = shiftIds.isEmpty()
                || (schedule.getShift() != null && shiftIds.contains(schedule.getShift().getId()));
        return matchesRole && matchesShift;
    }

    private Set<UUID> toIdSet(List<UUID> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().collect(Collectors.toSet());
    }

    private WorkSchedule cloneSchedule(WorkSchedule source, LocalDate targetDate) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStaff(source.getStaff());
        schedule.setWorkDate(targetDate);
        schedule.setShift(source.getShift());
        schedule.setExamRoom(source.getExamRoom());
        schedule.setStation(source.getStation());
        schedule.setRole(source.getRole());
        schedule.setCapacity(source.getCapacity());
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setNote(source.getNote());
        return schedule;
    }

    private WorkSchedule createGeneratedSchedule(Users staff, Shift shift, LocalDate targetDate) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStaff(staff);
        schedule.setWorkDate(targetDate);
        schedule.setShift(shift);
        schedule.setRole(staff.getRole());
        schedule.setCapacity(1);
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setNote("Tự xếp từ kế hoạch tuần");
        return schedule;
    }

    private WeeklySchedulePlanItemResponse toGeneratedPlanItem(
            Users staff,
            Shift shift,
            WorkSchedule created,
            LocalDate targetDate,
            boolean conflict,
            String conflictReason) {
        Roles role = staff.getRole();
        return new WeeklySchedulePlanItemResponse(
                null,
                created == null ? null : created.getId(),
                staff.getId(),
                staff.getFullName(),
                null,
                targetDate,
                shift.getId(),
                shift.getCode(),
                shift.getName(),
                role == null ? null : role.getId(),
                role == null ? null : role.getCode(),
                conflict,
                conflictReason
        );
    }

    private WeeklySchedulePlanItemResponse toWeeklyPlanItem(
            WorkSchedule source,
            WorkSchedule created,
            LocalDate targetDate,
            boolean conflict,
            String conflictReason) {
        return new WeeklySchedulePlanItemResponse(
                source.getId(),
                created == null ? null : created.getId(),
                source.getStaff() == null ? null : source.getStaff().getId(),
                source.getStaff() == null ? null : source.getStaff().getFullName(),
                source.getWorkDate(),
                targetDate,
                source.getShift() == null ? null : source.getShift().getId(),
                source.getShift() == null ? null : source.getShift().getCode(),
                source.getShift() == null ? null : source.getShift().getName(),
                source.getRole() == null ? null : source.getRole().getId(),
                source.getRole() == null ? null : source.getRole().getCode(),
                conflict,
                conflictReason
        );
    }

    private void validateWeeklyPlanRequest(WeeklySchedulePlanRequest request) {
        if (request.sourceWeekStart() == null || request.targetWeekStart() == null
                || request.sourceWeekStart().equals(request.targetWeekStart())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void applyRequest(WorkSchedule schedule, WorkScheduleRequest request, Users staff, Shift shift, Roles role) {
        schedule.setStaff(staff);
        schedule.setWorkDate(request.workDate());
        schedule.setShift(shift);
        schedule.setExamRoom(findExamRoom(request.examRoomId()));
        schedule.setStation(findStation(request.stationId()));
        schedule.setRole(role);
        schedule.setCapacity(request.capacity());
        schedule.setStatusCode(request.statusCode());
        schedule.setNote(request.note());
    }

    private WorkSchedule findSchedule(UUID scheduleId) {
        return workScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private Users findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private Shift findActiveShift(UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        if (!Boolean.TRUE.equals(shift.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        return shift;
    }

    private Roles findRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private ExamRoom findExamRoom(UUID examRoomId) {
        if (examRoomId == null) {
            return null;
        }
        return examRoomRepository.findById(examRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private GroomingStation findStation(UUID stationId) {
        if (stationId == null) {
            return null;
        }
        return groomingStationRepository.findById(stationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private record PlanCounts(int createdCount, int skippedCount) {
    }
}



