package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.schedule.dto.request.ShiftChangeRequestCreateRequest;
import com.astral.express.pccms.schedule.dto.request.ShiftChangeRespondRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShiftChangeRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShiftChangeRequestService shiftChangeRequestService;

    @InjectMocks
    private ShiftChangeRequestController shiftChangeRequestController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(shiftChangeRequestController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyRequests_success() throws Exception {
        given(shiftChangeRequestService.getMyRequests(any(), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of())));

        mockMvc.perform(get("/v1/me/shift-change-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRequest_success() throws Exception {
        given(shiftChangeRequestService.createRequest(any(ShiftChangeRequestCreateRequest.class)))
                .willReturn(null);

        mockMvc.perform(post("/v1/me/shift-change-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\":\"" + UUID.randomUUID() + "\",\"reason\":\"Sick\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelOwnRequest_success() throws Exception {
        UUID requestId = UUID.randomUUID();
        given(shiftChangeRequestService.cancelOwnRequest(requestId)).willReturn(null);

        mockMvc.perform(patch("/v1/me/shift-change-requests/{requestId}/cancel", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getIncomingRequests_success() throws Exception {
        given(shiftChangeRequestService.getIncomingRequests(any(), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of())));

        mockMvc.perform(get("/v1/shift-change-requests/incoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void respondToRequest_success() throws Exception {
        UUID requestId = UUID.randomUUID();
        given(shiftChangeRequestService.respondToRequest(eq(requestId), any())).willReturn(null);

        mockMvc.perform(patch("/v1/shift-change-requests/{requestId}/respond", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
