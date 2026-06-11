package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.service.WorkScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void should_ReturnValidationFailed_when_TC_SCH_005_missingRequiredFields() throws Exception {
        String request = """
                {
                  "staffId": null,
                  "workDate": null,
                  "shiftId": null,
                  "roleId": null,
                  "statusCode": null
                }
                """;

        mockMvc.perform(post("/v1/admin/work-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnValidationFailed_when_SearchSchedulesMissingDateRange() throws Exception {
        mockMvc.perform(get("/v1/admin/work-schedules"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnValidationFailed_when_TC_SCH_011_invalidScheduleStatus() throws Exception {
        String request = """
                {
                  "staffId": "00000000-0000-0000-0000-000000000002",
                  "workDate": "2026-04-12",
                  "shiftId": "00000000-0000-0000-0000-000000000001",
                  "roleId": "00000000-0000-0000-0000-000000000002",
                  "capacity": 1,
                  "statusCode": "PENDING"
                }
                """;

        mockMvc.perform(post("/v1/admin/work-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }
}
