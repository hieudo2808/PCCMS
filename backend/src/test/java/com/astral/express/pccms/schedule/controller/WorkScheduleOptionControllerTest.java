package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.schedule.dto.response.ExamRoomOptionResponse;
import com.astral.express.pccms.schedule.dto.response.GroomingStationOptionResponse;
import com.astral.express.pccms.schedule.dto.response.RoleOptionResponse;
import com.astral.express.pccms.schedule.dto.response.ShiftOptionResponse;
import com.astral.express.pccms.schedule.dto.response.StaffOptionResponse;
import com.astral.express.pccms.schedule.service.WorkScheduleOptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
        mockMvc = MockMvcBuilders.standaloneSetup(workScheduleOptionController).build();
    }

    @Test
    void should_ReturnStaffOptions() throws Exception {
        given(workScheduleOptionService.getStaffOptions())
                .willReturn(List.of(new StaffOptionResponse(id("1"), "Staff One", "STAFF", "Nhan vien")));

        mockMvc.perform(get("/v1/admin/work-schedules/options/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(id("1").toString()))
                .andExpect(jsonPath("$.data[0].fullName").value("Staff One"))
                .andExpect(jsonPath("$.data[0].roleCode").value("STAFF"));
    }

    @Test
    void should_ReturnShiftOptions() throws Exception {
        given(workScheduleOptionService.getShiftOptions())
                .willReturn(List.of(new ShiftOptionResponse(id("2"), "MORNING", "Morning", LocalTime.of(8, 0), LocalTime.of(12, 0))));

        mockMvc.perform(get("/v1/admin/work-schedules/options/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shiftCode").value("MORNING"))
                .andExpect(jsonPath("$.data[0].shiftName").value("Morning"));
    }

    @Test
    void should_ReturnRoleOptions() throws Exception {
        given(workScheduleOptionService.getRoleOptions())
                .willReturn(List.of(new RoleOptionResponse(id("3"), "VETERINARIAN", "Bac si")));

        mockMvc.perform(get("/v1/admin/work-schedules/options/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("VETERINARIAN"))
                .andExpect(jsonPath("$.data[0].name").value("Bac si"));
    }

    @Test
    void should_ReturnExamRoomOptions() throws Exception {
        given(workScheduleOptionService.getExamRoomOptions())
                .willReturn(List.of(new ExamRoomOptionResponse(id("4"), "EX01", "Exam room 1")));

        mockMvc.perform(get("/v1/admin/work-schedules/options/exam-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomCode").value("EX01"))
                .andExpect(jsonPath("$.data[0].name").value("Exam room 1"));
    }

    @Test
    void should_ReturnGroomingStationOptions() throws Exception {
        given(workScheduleOptionService.getGroomingStationOptions())
                .willReturn(List.of(new GroomingStationOptionResponse(id("5"), "GR01", "Station 1")));

        mockMvc.perform(get("/v1/admin/work-schedules/options/grooming-stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stationCode").value("GR01"))
                .andExpect(jsonPath("$.data[0].name").value("Station 1"));
    }

    private UUID id(String value) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", Long.parseLong(value)));
    }
}
