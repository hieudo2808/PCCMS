package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.service.BoardingService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingService boardingService;

    @InjectMocks
    private BoardingController boardingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(boardingController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAvailability_success() throws Exception {
        given(boardingService.getAvailability(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/boarding/availability")
                .param("startAt", "2024-01-01T00:00:00Z")
                .param("endAt", "2024-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createBooking_success() throws Exception {
        mockMvc.perform(post("/v1/boarding/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":\"" + UUID.randomUUID() + "\",\"roomTypeId\":\"" + UUID.randomUUID() + "\",\"expectedCheckinAt\":\"2034-01-01T00:00:00Z\",\"expectedCheckoutAt\":\"2034-01-02T00:00:00Z\",\"specialCareRequest\":\"Note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void listMyBookings_success() throws Exception {
        PageResponse<BoardingBookingResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(boardingService.listMyBookings(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/boarding/bookings/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listBookings_success() throws Exception {
        PageResponse<BoardingBookingResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(boardingService.listBookings(any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/boarding/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmBooking_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/bookings/{bookingId}/confirmations", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void checkIn_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/bookings/{bookingId}/check-ins", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void startStay_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/bookings/{bookingId}/stay-starts", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void checkOut_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/bookings/{bookingId}/check-outs", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelBooking_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/bookings/{bookingId}/cancellations", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Cancel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createCareLog_success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        mockMvc.perform(post("/v1/boarding/sessions/{sessionId}/care-logs", sessionId)
                .param("logDate", "2024-01-01")
                .param("periodCode", "MORNING")
                .param("feedingStatus", "NORMAL")
                .param("hygieneStatus", "CLEAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void listCareLogs_success() throws Exception {
        UUID bookingId = UUID.randomUUID();
        mockMvc.perform(get("/v1/boarding/bookings/{bookingId}/care-logs", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
