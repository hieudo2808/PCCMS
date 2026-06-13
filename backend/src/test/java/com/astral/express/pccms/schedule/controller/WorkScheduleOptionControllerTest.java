package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.service.WorkScheduleOptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkScheduleOptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkScheduleOptionService workScheduleOptionService;

    @InjectMocks
    private WorkScheduleOptionController workScheduleOptionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workScheduleOptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStaffOptions_success() throws Exception {
        given(workScheduleOptionService.getStaffOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPublicStaffOptions_success() throws Exception {
        given(workScheduleOptionService.getStaffOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/public-staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getShiftOptions_success() throws Exception {
        given(workScheduleOptionService.getShiftOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRoleOptions_success() throws Exception {
        given(workScheduleOptionService.getRoleOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getExamRoomOptions_success() throws Exception {
        given(workScheduleOptionService.getExamRoomOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/exam-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getGroomingStationOptions_success() throws Exception {
        given(workScheduleOptionService.getGroomingStationOptions()).willReturn(List.of());

        mockMvc.perform(get("/v1/admin/work-schedules/options/grooming-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
