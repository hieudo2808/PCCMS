package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.service.WorkScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkScheduleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkScheduleService workScheduleService;

    @InjectMocks
    private WorkScheduleController workScheduleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workScheduleController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchSchedules_success() throws Exception {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().plusDays(7);
        
        given(workScheduleService.searchSchedules(eq(fromDate), eq(toDate), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of())));

        mockMvc.perform(get("/v1/admin/work-schedules")
                        .param("fromDate", fromDate.toString())
                        .param("toDate", toDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createSchedule_success() throws Exception {
        given(workScheduleService.createSchedule(any(WorkScheduleRequest.class))).willReturn(null);

        mockMvc.perform(post("/v1/admin/work-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"" + UUID.randomUUID() + "\",\"workDate\":\"2025-01-01\",\"shiftId\":\"" + UUID.randomUUID() + "\",\"roleId\":\"" + UUID.randomUUID() + "\",\"capacity\":10,\"statusCode\":\"ASSIGNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void previewWeeklyPlan_success() throws Exception {
        given(workScheduleService.previewWeeklyPlan(any(WeeklySchedulePlanRequest.class))).willReturn(null);

        mockMvc.perform(post("/v1/admin/work-schedules/weekly-plan/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceWeekStart\":\"2025-01-01\",\"targetWeekStart\":\"2025-01-08\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void applyWeeklyPlan_success() throws Exception {
        given(workScheduleService.applyWeeklyPlan(any(WeeklySchedulePlanRequest.class))).willReturn(null);

        mockMvc.perform(post("/v1/admin/work-schedules/weekly-plan/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceWeekStart\":\"2025-01-01\",\"targetWeekStart\":\"2025-01-08\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateSchedule_success() throws Exception {
        UUID scheduleId = UUID.randomUUID();
        given(workScheduleService.updateSchedule(eq(scheduleId), any(WorkScheduleRequest.class))).willReturn(null);

        mockMvc.perform(put("/v1/admin/work-schedules/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"" + UUID.randomUUID() + "\",\"workDate\":\"2025-01-01\",\"shiftId\":\"" + UUID.randomUUID() + "\",\"roleId\":\"" + UUID.randomUUID() + "\",\"capacity\":10,\"statusCode\":\"ASSIGNED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelSchedule_success() throws Exception {
        UUID scheduleId = UUID.randomUUID();
        given(workScheduleService.cancelSchedule(scheduleId)).willReturn(null);

        mockMvc.perform(delete("/v1/admin/work-schedules/{scheduleId}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
