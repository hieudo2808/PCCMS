package com.astral.express.pccms.grooming.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCancelRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.grooming.service.GroomingService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GroomingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroomingService groomingService;

    @InjectMocks
    private GroomingController groomingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groomingController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listActiveServices_success() throws Exception {
        given(groomingService.listActiveServices()).willReturn(List.of());

        mockMvc.perform(get("/v1/grooming/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listActiveStations_success() throws Exception {
        given(groomingService.listActiveStations()).willReturn(List.of());

        mockMvc.perform(get("/v1/grooming/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createBooking_success() throws Exception {
        mockMvc.perform(post("/v1/grooming/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":\"" + UUID.randomUUID() + "\",\"serviceId\":\"" + UUID.randomUUID() + "\",\"scheduledStartAt\":\"2034-01-01T00:00:00Z\",\"ownerNote\":\"Note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void listMyTickets_success() throws Exception {
        PageResponse<GroomingTicketResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(groomingService.listMyTickets(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/grooming/tickets/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyTicket_success() throws Exception {
        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(get("/v1/grooming/tickets/my/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listTickets_success() throws Exception {
        PageResponse<GroomingTicketResponse> page = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        given(groomingService.listTickets(any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/grooming/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmTicket_success() throws Exception {
        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(post("/v1/grooming/tickets/{ticketId}/confirmations", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stationId\":\"" + UUID.randomUUID() + "\",\"internalNote\":\"Note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void startTicket_success() throws Exception {
        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(post("/v1/grooming/tickets/{ticketId}/starts", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void completeTicket_success() throws Exception {
        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(post("/v1/grooming/tickets/{ticketId}/completions", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"internalNote\":\"Note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelTicket_success() throws Exception {
        UUID ticketId = UUID.randomUUID();
        mockMvc.perform(post("/v1/grooming/tickets/{ticketId}/cancellations", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Cancel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listGroomingServicesForAdmin_success() throws Exception {
        given(groomingService.listGroomingServicesForAdmin()).willReturn(List.of());

        mockMvc.perform(get("/v1/grooming/admin/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createGroomingService_success() throws Exception {
        mockMvc.perform(post("/v1/grooming/admin/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"C1\",\"name\":\"Service\",\"description\":\"Desc\",\"basePriceVnd\":100,\"durationMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void updateGroomingService_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/grooming/admin/services/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceCode\":\"C1\",\"name\":\"Service\",\"description\":\"Desc\",\"basePriceVnd\":100,\"durationMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivateGroomingService_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/grooming/admin/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listStationsForAdmin_success() throws Exception {
        given(groomingService.listStationsForAdmin()).willReturn(List.of());

        mockMvc.perform(get("/v1/grooming/admin/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createStation_success() throws Exception {
        mockMvc.perform(post("/v1/grooming/admin/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stationCode\":\"S1\",\"name\":\"Station\",\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void updateStation_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(put("/v1/grooming/admin/stations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stationCode\":\"S1\",\"name\":\"Station\",\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivateStation_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/grooming/admin/stations/{id}/deactivation", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
