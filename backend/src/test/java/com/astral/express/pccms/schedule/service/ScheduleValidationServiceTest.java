package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleValidationServiceTest {
    private final ScheduleValidationService service = new ScheduleValidationService();

    @Test
    void shouldAcceptPositiveCapacity() {
        assertThatCode(() -> service.validateCapacity(1)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullOrNonPositiveCapacity() {
        assertThatThrownBy(() -> service.validateCapacity(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validateCapacity(0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validateCapacity(-1)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        LocalDate today = LocalDate.of(2026, 6, 17);

        assertThatThrownBy(() -> service.validateDateRange(today, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validateDateRange(today, today.minusDays(1))).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectWeeklyPlanWithMissingOrSameWeeks() {
        LocalDate today = LocalDate.of(2026, 6, 17);

        assertThatThrownBy(() -> service.validateWeeklyPlanRequest(new WeeklySchedulePlanRequest(null, today, List.of(), List.of())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validateWeeklyPlanRequest(new WeeklySchedulePlanRequest(today, today, List.of(), List.of())))
                .isInstanceOf(BusinessException.class);
    }
}
