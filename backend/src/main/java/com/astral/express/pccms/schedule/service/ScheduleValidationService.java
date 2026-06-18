package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ScheduleValidationService {
    public void validateCapacity(Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    public void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    public void validateWeeklyPlanRequest(WeeklySchedulePlanRequest request) {
        if (request.sourceWeekStart() == null || request.targetWeekStart() == null
                || request.sourceWeekStart().equals(request.targetWeekStart())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }
}
