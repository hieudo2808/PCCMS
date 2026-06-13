package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.dto.request.ShiftRequestStatusUpdateRequest;
import com.astral.express.pccms.schedule.dto.response.ShiftChangeRequestResponse;
import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import com.astral.express.pccms.schedule.service.ShiftChangeRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminShiftChangeRequestControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ShiftChangeRequestService shiftChangeRequestService;

    @InjectMocks
    private AdminShiftChangeRequestController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAdminRequests_success() throws Exception {
        ShiftChangeRequestResponse response = new ShiftChangeRequestResponse(UUID.randomUUID(), UUID.randomUUID(), "Requester", null, null, null, null, ShiftRequestStatus.PENDING, null, null, null);
        given(shiftChangeRequestService.getAdminRequests(any(), any())).willReturn(PageResponse.of(new PageImpl<>(List.of(response))));

        mockMvc.perform(get("/v1/admin/shift-change-requests")
                        .param("statusCode", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateRequestStatus_success() throws Exception {
        UUID requestId = UUID.randomUUID();
        ShiftRequestStatusUpdateRequest request = new ShiftRequestStatusUpdateRequest(ShiftRequestStatus.ACCEPTED);
        ShiftChangeRequestResponse response = new ShiftChangeRequestResponse(requestId, UUID.randomUUID(), "Requester", null, null, null, null, ShiftRequestStatus.ACCEPTED, null, null, null);

        given(shiftChangeRequestService.updateRequestStatus(any(), any())).willReturn(response);

        mockMvc.perform(patch("/v1/admin/shift-change-requests/{requestId}/status", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
