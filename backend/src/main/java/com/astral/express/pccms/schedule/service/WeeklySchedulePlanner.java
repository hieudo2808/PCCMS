package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanItemResponse;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.ShiftRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklySchedulePlanner {
    private static final int DAYS_IN_WEEK = 7;
    private static final List<String> SCHEDULABLE_ROLE_CODES = List.of("STAFF", "VETERINARIAN", "RECEPTIONIST");

    private final WorkScheduleRepository workScheduleRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final ExamRoomRepository examRoomRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final ScheduleConflictChecker scheduleConflictChecker;
    private final ScheduleValidationService scheduleValidationService;
    private final WorkplaceAssignmentPolicy workplaceAssignmentPolicy;

    public WeeklySchedulePlanResponse previewWeeklyPlan(WeeklySchedulePlanRequest request) {
        return buildWeeklyPlan(request, false);
    }

    @Transactional
    public WeeklySchedulePlanResponse applyWeeklyPlan(WeeklySchedulePlanRequest request) {
        return buildWeeklyPlan(request, true);
    }

    private WeeklySchedulePlanResponse buildWeeklyPlan(WeeklySchedulePlanRequest request, boolean persist) {
        scheduleValidationService.validateWeeklyPlanRequest(request);

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
            boolean conflict = scheduleConflictChecker.hasStaffShiftConflict(
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

        List<ExamRoom> examRooms = examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc();
        List<GroomingStation> stations = groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc();
        WorkplaceAssignmentPolicy.Cursor workplaceCursor = workplaceAssignmentPolicy.createCursor(examRooms, stations);

        int createdCount = 0;
        int skippedCount = 0;
        for (int dayOffset = 0; dayOffset < DAYS_IN_WEEK; dayOffset++) {
            LocalDate targetDate = request.targetWeekStart().plusDays(dayOffset);
            for (int staffIndex = 0; staffIndex < staffMembers.size(); staffIndex++) {
                Users staff = staffMembers.get(staffIndex);
                Shift shift = shifts.get((dayOffset + staffIndex) % shifts.size());
                boolean conflict = scheduleConflictChecker.hasStaffShiftConflict(
                        staff.getId(), targetDate, shift.getId());

                if (conflict) {
                    skippedCount++;
                    items.add(toGeneratedPlanItem(staff, shift, null, targetDate, true, "STAFF_SHIFT_ALREADY_EXISTS"));
                    continue;
                }

                WorkplaceAssignmentPolicy.WorkplaceAssignment workplace = workplaceCursor.assignFor(staff);

                WorkSchedule created = null;
                if (persist) {
                    created = createGeneratedSchedule(staff, shift, targetDate, workplace.examRoom(), workplace.station());
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
        return new HashSet<>(values);
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

    private WorkSchedule createGeneratedSchedule(Users staff, Shift shift, LocalDate targetDate, ExamRoom room, GroomingStation station) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStaff(staff);
        schedule.setWorkDate(targetDate);
        schedule.setShift(shift);
        schedule.setRole(staff.getRole());
        schedule.setExamRoom(room);
        schedule.setStation(station);
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

    private record PlanCounts(int createdCount, int skippedCount) {
    }
}
