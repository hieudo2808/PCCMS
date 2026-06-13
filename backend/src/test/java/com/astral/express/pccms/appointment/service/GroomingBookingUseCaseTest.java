package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateGroomingAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.UpdateGroomingStatusRequest;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.service.GroomingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroomingBookingUseCaseTest {

    @Mock
    private GroomingService groomingService;

    @Mock
    private GroomingTicketRepository groomingTicketRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private AppointmentResponseAssembler assembler;

    @InjectMocks
    private GroomingBookingUseCase useCase;

    private UUID ownerId;
    private UUID ticketId;
    private GroomingTicket ticket;
    private Appointment appointment;
    private ServiceOrder order;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        
        order = new ServiceOrder();
        order.setId(UUID.randomUUID());
        order.setStatusCode(ServiceOrderStatus.REQUESTED);
        
        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatusCode(AppointmentStatus.PENDING);
        appointment.setServiceOrder(order);
        
        ticket = new GroomingTicket();
        ticket.setId(ticketId);
        ticket.setStatusCode(GroomingStatus.PENDING);
        ticket.setAppointment(appointment);
    }

    @Test
    void should_CreateGroomingAppointment_Successfully() {
        // GIVEN
        CreateGroomingAppointmentRequest request = new CreateGroomingAppointmentRequest(
                UUID.randomUUID(), "GROOM_FULL", LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Note"
        );
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setId(UUID.randomUUID());
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("GROOM_FULL"))
                .willReturn(Optional.of(catalog));

        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse dedicatedResponse =
                org.mockito.Mockito.mock(com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse.class);
        given(dedicatedResponse.id()).willReturn(ticketId);
        given(groomingService.createBooking(any())).willReturn(dedicatedResponse);
        given(groomingTicketRepository.findById(ticketId)).willReturn(Optional.of(ticket));
        
        AppointmentResponse expectedResponse = org.mockito.Mockito.mock(AppointmentResponse.class);
        given(expectedResponse.id()).willReturn(appointment.getId());
        given(assembler.toResponse(appointment, null)).willReturn(expectedResponse);

        // WHEN
        AppointmentResponse response = useCase.createGroomingAppointment(request, ownerId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(appointment.getId());
    }

    @Test
    void should_ThrowException_when_CreateGroomingAppointment_ServiceNotFound() {
        // GIVEN
        CreateGroomingAppointmentRequest request = new CreateGroomingAppointmentRequest(
                UUID.randomUUID(), "INVALID", LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Note"
        );
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("INVALID"))
                .willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> useCase.createGroomingAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND);
    }

    @Test
    void should_ListGroomingBoard_Successfully() {
        // GIVEN
        given(groomingTicketRepository.findBoardForDate(any(), any(), any())).willReturn(List.of(ticket));
        
        GroomingBoardCardResponse expected = org.mockito.Mockito.mock(GroomingBoardCardResponse.class);
        given(expected.ticketId()).willReturn(ticketId);
        given(assembler.toGroomingBoardCard(ticket)).willReturn(expected);

        // WHEN
        List<GroomingBoardCardResponse> responses = useCase.listGroomingBoard(LocalDate.now());

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).ticketId()).isEqualTo(ticketId);
    }

    @Test
    void should_UpdateGroomingStatus_to_Confirmed_Successfully() {
        // GIVEN
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.CONFIRMED);
        
        GroomingBoardCardResponse expected = org.mockito.Mockito.mock(GroomingBoardCardResponse.class);
        given(expected.ticketId()).willReturn(ticketId);
        given(assembler.toGroomingBoardCard(ticket)).willReturn(expected);

        // WHEN
        GroomingBoardCardResponse response = useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.CONFIRMED);
    }

    @Test
    void should_ThrowException_when_InvalidTransition() {
        // GIVEN
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.COMPLETED);

        // WHEN & THEN
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_UpdateGroomingStatus_to_InService_Successfully() {
        // GIVEN
        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.IN_SERVICE);

        // WHEN
        useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.IN_SERVICE);
        assertThat(ticket.getStartedAt()).isNotNull();
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void should_UpdateGroomingStatus_to_Completed_Successfully() {
        // GIVEN
        ticket.setStatusCode(GroomingStatus.IN_SERVICE);
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.COMPLETED);

        // WHEN
        useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.COMPLETED);
        assertThat(ticket.getCompletedAt()).isNotNull();
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    void should_UpdateGroomingStatus_to_Cancelled_Successfully() {
        // GIVEN
        ticket.setStatusCode(GroomingStatus.PENDING);
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.CANCELLED);

        // WHEN
        useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.CANCELLED);
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    void should_ThrowException_when_CreateGroomingAppointment_TicketNotFound() {
        // GIVEN
        CreateGroomingAppointmentRequest request = new CreateGroomingAppointmentRequest(
                UUID.randomUUID(), "GROOM_FULL", LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Note"
        );
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setId(UUID.randomUUID());
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("GROOM_FULL"))
                .willReturn(Optional.of(catalog));

        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse dedicatedResponse =
                org.mockito.Mockito.mock(com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse.class);
        given(dedicatedResponse.id()).willReturn(ticketId);
        given(groomingService.createBooking(any())).willReturn(dedicatedResponse);
        given(groomingTicketRepository.findById(ticketId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> useCase.createGroomingAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_001_NOT_FOUND);
    }

    @Test
    void should_ListGroomingBoard_WithNullDate() {
        // GIVEN
        given(groomingTicketRepository.findBoardForDate(any(), any(), any())).willReturn(List.of(ticket));
        
        // WHEN
        List<GroomingBoardCardResponse> responses = useCase.listGroomingBoard(null);

        // THEN
        assertThat(responses).hasSize(1);
    }

    @Test
    void should_ThrowException_when_UpdateGroomingStatus_TicketNotFound() {
        // GIVEN
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.empty());
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.CONFIRMED);

        // WHEN & THEN
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_001_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_InvalidTransitions() {
        UpdateGroomingStatusRequest requestPending = new UpdateGroomingStatusRequest(GroomingStatus.PENDING);
        
        // From CANCELLED
        ticket.setStatusCode(GroomingStatus.CANCELLED);
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, requestPending))
                .isInstanceOf(BusinessException.class);
                
        // From COMPLETED
        ticket.setStatusCode(GroomingStatus.COMPLETED);
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, requestPending))
                .isInstanceOf(BusinessException.class);

        // From CONFIRMED to PENDING
        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, requestPending))
                .isInstanceOf(BusinessException.class);

        // From IN_SERVICE to PENDING
        ticket.setStatusCode(GroomingStatus.IN_SERVICE);
        assertThatThrownBy(() -> useCase.updateGroomingStatus(ticketId, requestPending))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_UpdateGroomingStatus_to_Cancelled_From_Confirmed_Successfully() {
        // GIVEN
        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.CANCELLED);

        // WHEN
        useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.CANCELLED);
    }

    @Test
    void should_UpdateGroomingStatus_to_InService_WhenAlreadyStarted() {
        // GIVEN
        ticket.setStatusCode(GroomingStatus.CONFIRMED);
        ticket.setStartedAt(OffsetDateTime.now());
        given(groomingTicketRepository.findDetailById(ticketId)).willReturn(Optional.of(ticket));
        UpdateGroomingStatusRequest request = new UpdateGroomingStatusRequest(GroomingStatus.IN_SERVICE);

        // WHEN
        useCase.updateGroomingStatus(ticketId, request);

        // THEN
        assertThat(ticket.getStatusCode()).isEqualTo(GroomingStatus.IN_SERVICE);
    }

}