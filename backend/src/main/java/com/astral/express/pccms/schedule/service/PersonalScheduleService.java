package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.schedule.service.PersonalScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalScheduleService {
    private final WorkScheduleRepository workScheduleRepository;
    private final SecurityContextService SecurityContextService;
@PreAuthorize("isAuthenticated()")
    public PageResponse<WorkScheduleResponse> getMySchedules(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        UUID currentUserId = currentUserId();
        validateDateRange(fromDate, toDate);
        Page<WorkSchedule> schedules = workScheduleRepository.findByStaffIdAndWorkDateBetween(
                currentUserId, fromDate, toDate, pageable);
        return PageResponse.of(schedules.map(ScheduleMapperSupport::toWorkScheduleResponse));
    }
@PreAuthorize("isAuthenticated()")
    public PageResponse<WorkScheduleResponse> getStaffSchedules(
            UUID staffId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        UUID currentUserId = currentUserId();
        if (!currentUserId.equals(staffId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        validateDateRange(fromDate, toDate);
        Page<WorkSchedule> schedules = workScheduleRepository.findByStaffIdAndWorkDateBetween(
                staffId, fromDate, toDate, pageable);
        return PageResponse.of(schedules.map(ScheduleMapperSupport::toWorkScheduleResponse));
    }

    private UUID currentUserId() {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }
}


