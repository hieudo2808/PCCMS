package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.dto.response.ShiftChangeRequestResponse;
import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import com.astral.express.pccms.schedule.service.ShiftChangeRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShiftChangeRequestControllerTest {

    private MockMvc userMockMvc;
    private MockMvc adminMockMvc;

    @Mock
    private ShiftChangeRequestService shiftChangeRequestService;

    @InjectMocks
    private ShiftChangeRequestController shiftChangeRequestController;

    @InjectMocks
    private AdminShiftChangeRequestController adminShiftChangeRequestController;

    @BeforeEach
    void setUp() {
        userMockMvc = MockMvcBuilders.standaloneSetup(shiftChangeRequestController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        adminMockMvc = MockMvcBuilders.standaloneSetup(adminShiftChangeRequestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnOwnShiftChangeRequests_when_GetMyRequests() throws Exception {
        ShiftChangeRequestResponse response = new ShiftChangeRequestResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                null,
                "Family matter",
                ShiftRequestStatus.PENDING,
                null,
                null,
                OffsetDateTime.parse("2026-04-01T00:00:00Z")
        );
        given(shiftChangeRequestService.getMyRequests(isNull(), any()))
                .willReturn(PageResponse.of(new PageImpl<>(List.of(response))));

        userMockMvc.perform(get("/v1/me/shift-change-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data.content[0].statusCode").value("PENDING"));
    }

    @Test
    void should_ReturnValidationFailed_when_TC_SHIFT_REQ_005_blankReason() throws Exception {
        String request = """
                {
                  "scheduleId": "00000000-0000-0000-0000-000000000010",
                  "reason": " ",
                  "targetStaffId": null
                }
                """;

        userMockMvc.perform(post("/v1/me/shift-change-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }

    @Test
    void should_ReturnValidationFailed_when_TC_SHIFT_REQ_011_invalidRequestStatus() throws Exception {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        String request = """
                {
                  "statusCode": "APPROVED"
                }
                """;

        adminMockMvc.perform(patch("/v1/admin/shift-change-requests/{requestId}/status", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.ERR_400_BAD_REQUEST.getErrorCode()));
    }
}
