package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PersonalWorkScheduleOptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkScheduleOptionService workScheduleOptionService;

    @InjectMocks
    private PersonalWorkScheduleOptionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getStaffOptions_success() throws Exception {
        StaffOptionResponse response = new StaffOptionResponse(UUID.randomUUID(), "Test Staff", "VETERINARIAN", "Veterinarian");
        given(workScheduleOptionService.getStaffOptions()).willReturn(List.of(response));

        mockMvc.perform(get("/v1/work-schedules/options/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fullName").value("Test Staff"));
    }
}
