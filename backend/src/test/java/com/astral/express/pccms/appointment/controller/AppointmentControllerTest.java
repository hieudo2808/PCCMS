package com.astral.express.pccms.appointment.controller;

import com.astral.express.pccms.appointment.dto.request.*;
import com.astral.express.pccms.appointment.dto.response.*;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.service.*;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.mockito.Mockito;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private AppointmentAvailabilityUseCase availabilityUseCase;
    @Mock private AppointmentLifecycleUseCase lifecycleUseCase;
    @Mock private AppointmentQueryUseCase queryUseCase;
    @Mock private BoardingBookingUseCase boardingBookingUseCase;
    @Mock private GroomingBookingUseCase groomingBookingUseCase;
    @Mock private CreateMedicalAppointmentUseCase createMedicalAppointmentUseCase;
    @Mock private QuickCheckInUseCase quickCheckInUseCase;
    @Mock private AppointmentResponseAssembler appointmentResponseAssembler;
    @Mock private SecurityContextService securityContextService;

    @InjectMocks
    private AppointmentController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createMedicalAppointment_success() throws Exception {
        CreateMedicalAppointmentRequest request = Mockito.mock(CreateMedicalAppointmentRequest.class);
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(createMedicalAppointmentUseCase.createMedicalAppointment(any(), any())).willReturn(new Appointment());
        given(appointmentResponseAssembler.toResponse(any(), any())).willReturn(Mockito.mock(AppointmentResponse.class));

        mockMvc.perform(post("/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"appointmentDate\":\"2026-06-12\",\"slotStart\":\"10:00:00\",\"symptomText\":\"Sick\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void listOwnerAppointments_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(queryUseCase.listOwnerAppointments(any(), any())).willReturn(PageResponse.of(new PageImpl<>(List.of())));

        mockMvc.perform(get("/v1/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    void listTodayAppointments_success() throws Exception {
        given(queryUseCase.listTodayAppointments(any(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/today"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableSlots_success() throws Exception {
        given(availabilityUseCase.getAvailableSlots(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/slots").param("date", "2026-06-12"))
                .andExpect(status().isOk());
    }

    @Test
    void listAvailableVets_success() throws Exception {
        given(availabilityUseCase.listAvailableVets(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/vets").param("date", "2026-06-12").param("slotStart", "10:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    void listVetsOnDuty_success() throws Exception {
        given(availabilityUseCase.listVetsOnDuty(any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/vets/on-duty").param("date", "2026-06-12"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailabilitySummary_success() throws Exception {
        given(availabilityUseCase.getAvailabilitySummary(any(), any())).willReturn(Mockito.mock(AvailabilitySummaryResponse.class));

        mockMvc.perform(get("/v1/appointments/availability").param("date", "2026-06-12"))
                .andExpect(status().isOk());
    }

    @Test
    void checkIn_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(lifecycleUseCase.checkIn(any(), any())).willReturn(null);

        mockMvc.perform(post("/v1/appointments/{id}/check-in", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void startExam_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(lifecycleUseCase.startExam(any(), any())).willReturn(null);

        mockMvc.perform(post("/v1/appointments/{id}/start-exam", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(securityContextService.isAdminOrStaff()).willReturn(true);
        given(lifecycleUseCase.cancel(any(), any(), any(Boolean.class))).willReturn(null);

        mockMvc.perform(post("/v1/appointments/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void quickCheckIn_success() throws Exception {
        QuickCheckInRequest request = Mockito.mock(QuickCheckInRequest.class);
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(quickCheckInUseCase.execute(any(), any())).willReturn(new QuickCheckInUseCase.Result(new Appointment(), 1));

        mockMvc.perform(post("/v1/appointments/quick-check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0912345678\",\"petId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getVetQueue_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(queryUseCase.getVetQueue(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/queue"))
                .andExpect(status().isOk());
    }

    @Test
    void lookupCustomer_success() throws Exception {
        given(queryUseCase.lookupCustomerByPhone(any())).willReturn(new CustomerLookupResponse(UUID.randomUUID(), "John", "0901234567", List.of()));

        mockMvc.perform(get("/v1/appointments/customer-lookup").param("phone", "0901234567"))
                .andExpect(status().isOk());
    }

    @Test
    void createGroomingAppointment_success() throws Exception {
        CreateGroomingAppointmentRequest request = Mockito.mock(CreateGroomingAppointmentRequest.class);
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(groomingBookingUseCase.createGroomingAppointment(any(), any())).willReturn(null);

        mockMvc.perform(post("/v1/appointments/grooming")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"serviceCode\":\"G01\",\"appointmentDate\":\"2026-06-12\",\"slotStart\":\"10:00:00\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void listGroomingBoard_success() throws Exception {
        given(groomingBookingUseCase.listGroomingBoard(any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/grooming/board"))
                .andExpect(status().isOk());
    }

    @Test
    void updateGroomingStatus_success() throws Exception {
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(com.astral.express.pccms.appointment.entity.GroomingStatus.COMPLETED);
        given(groomingBookingUseCase.updateGroomingStatus(any(), any())).willReturn(null);

        mockMvc.perform(patch("/v1/appointments/grooming/{id}/status", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createBoardingBooking_success() throws Exception {
        CreateBoardingBookingRequest request = Mockito.mock(CreateBoardingBookingRequest.class);
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(boardingBookingUseCase.createBoardingBooking(any(), any())).willReturn(null);

        mockMvc.perform(post("/v1/appointments/boarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"petId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"roomTypeId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"checkinDate\":\"2026-06-12\",\"checkoutDate\":\"2026-06-13\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void listOwnerBoardingBookings_success() throws Exception {
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(boardingBookingUseCase.listOwnerBoardingBookings(any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/boarding"))
                .andExpect(status().isOk());
    }

    @Test
    void listRoomTypes_success() throws Exception {
        given(boardingBookingUseCase.listActiveRoomTypes()).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/room-types"))
                .andExpect(status().isOk());
    }

    @Test
    void listServices_success() throws Exception {
        given(queryUseCase.listServicesByCategory(any())).willReturn(List.of());

        mockMvc.perform(get("/v1/appointments/services").param("category", "MEDICAL"))
                .andExpect(status().isOk());
    }
}
