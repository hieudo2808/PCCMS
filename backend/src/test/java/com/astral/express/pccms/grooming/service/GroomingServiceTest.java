package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.service.RoomAvailabilityChecker;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCancelRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroomingServiceTest {

    @Mock
    private SecurityContextService SecurityContextService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private GroomingTicketRepository groomingTicketRepository;

    @Mock
    private GroomingStationRepository groomingStationRepository;

    @Mock
    private BillingHandoffService billingHandoffService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RoomAvailabilityChecker roomAvailabilityChecker;

    private GroomingService groomingService;

    @BeforeEach
    void setUp() {
        groomingService = new GroomingService(
                SecurityContextService,
                userRepository,
                petRepository,
                serviceCatalogRepository,
                serviceOrderRepository,
                appointmentRepository,
                groomingTicketRepository,
                groomingStationRepository,
                billingHandoffService,
                invoiceRepository,
                new GroomingMapper(),
                roomAvailabilityChecker);
    }

    @ParameterizedTest(name = "[{0}] {1}: {6}")
    @CsvFileSource(resources = "/testcases/grooming-service-lifecycle.csv", numLinesToSkip = 1)
    void should_enforce_grooming_lifecycle_rules(
            String ruleId,
            String caseId,
            String action,
            String mockState,
            String expectedResult,
            String expectedError,
            String note) {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        OffsetDateTime futureStart = OffsetDateTime.now().plusDays(1);

        Users owner = Users.builder().id(ownerId).fullName("Owner").build();
        Users otherOwner = Users.builder().id(UUID.randomUUID()).fullName("Other").build();
        Users staff = Users.builder().id(UUID.randomUUID()).fullName("Staff").build();
        Pets pet = Pets.builder()
                .id(petId)
                .name("Milu")
                .owner("PET_OTHER_OWNER".equals(mockState) ? otherOwner : owner)
                .build();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(serviceId);
        service.setServiceCode("GRM-BATH");
        service.setName("Tam say");
        service.setCategoryCode(ServiceCategory.GROOMING);
        service.setBasePriceVnd(100000L);
        service.setDurationMinutes(60);
        service.setIsActive(true);

        if ("CREATE_BOOKING".equals(action)) {
            given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
            given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            if (!"PET_OTHER_OWNER".equals(mockState)) {
                given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(serviceId, ServiceCategory.GROOMING))
                        .willReturn(Optional.of(service));
            }
            if ("VALID".equals(mockState) || "DUPLICATE_BOOKING".equals(mockState)) {
                given(groomingTicketRepository.existsOwnerBookingConflict(
                        eq(ownerId),
                        eq(petId),
                        eq(serviceId),
                        anyCollection(),
                        any(),
                        any()))
                        .willReturn("DUPLICATE_BOOKING".equals(mockState));
            }
            if ("VALID".equals(mockState)) {
                given(serviceOrderRepository.save(any(ServiceOrder.class))).willAnswer(invocation -> {
                    ServiceOrder order = invocation.getArgument(0);
                    assertThat(order.getRequestedAt()).isNotNull();
                    assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.REQUESTED);
                    order.setId(UUID.randomUUID());
                    return order;
                });
                given(appointmentRepository.save(any(Appointment.class))).willAnswer(invocation -> {
                    Appointment appointment = invocation.getArgument(0);
                    appointment.setId(UUID.randomUUID());
                    return appointment;
                });
                given(groomingTicketRepository.save(any(GroomingTicket.class))).willAnswer(invocation -> {
                    GroomingTicket ticket = invocation.getArgument(0);
                    ticket.setId(ticketId);
                    return ticket;
                });
                given(invoiceRepository.findByServiceOrderId(any(UUID.class))).willReturn(Optional.empty());
            }
        }

        if ("CONFIRM_TICKET".equals(action)) {
            GroomingTicket ticket = buildTicket(ticketId, owner, pet, service, GroomingStatus.PENDING);
            GroomingStation station = GroomingStation.builder()
                    .id(stationId)
                    .stationCode("SPA-01")
                    .name("Bàn spa 1")
                    .isActive(true)
                    .build();
            given(SecurityContextService.getCurrentUserId()).willReturn(staff.getId());
            given(userRepository.findById(staff.getId())).willReturn(Optional.of(staff));
            given(groomingTicketRepository.findLockedWithDetailsById(ticketId)).willReturn(Optional.of(ticket));
            given(groomingStationRepository.findWithLockById(stationId)).willReturn(Optional.of(station));
            given(groomingTicketRepository.existsStationConflict(eq(stationId), anyCollection(), any(), any(), eq(ticketId)))
                    .willReturn("STATION_CONFLICT".equals(mockState));
        }

        if ("COMPLETE_TICKET".equals(action)) {
            GroomingTicket ticket = buildTicket(ticketId, owner, pet, service, GroomingStatus.IN_SERVICE);
            Invoice invoice = Invoice.builder()
                    .id(UUID.randomUUID())
                    .invoiceCode("INV-001")
                    .owner(owner)
                    .pet(pet)
                    .totalAmountVnd(100000L)
                    .paidAmountVnd(0L)
                    .build();
            given(SecurityContextService.getCurrentUserId()).willReturn(staff.getId());
            given(userRepository.findById(staff.getId())).willReturn(Optional.of(staff));
            given(groomingTicketRepository.findLockedWithDetailsById(ticketId)).willReturn(Optional.of(ticket));
            given(groomingTicketRepository.save(any(GroomingTicket.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(billingHandoffService.createGroomingInvoice(any(ServiceOrder.class), any(Appointment.class), any(GroomingTicket.class), eq(staff)))
                    .willReturn(invoice);
            given(invoiceRepository.findByServiceOrderId(ticket.getAppointment().getServiceOrder().getId())).willReturn(Optional.of(invoice));
        }

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> execute(action, mockState, petId, serviceId, futureStart, ticketId, stationId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedError));
            if ("STATION_CONFLICT".equals(mockState)) {
                verify(serviceOrderRepository, never()).save(any(ServiceOrder.class));
            }
            return;
        }

        var response = execute(action, mockState, petId, serviceId, futureStart, ticketId, stationId);

        assertThat(response).isNotNull();
        if ("CREATE_BOOKING".equals(action)) {
            assertThat(response.statusCode()).isEqualTo(GroomingStatus.PENDING);
            assertThat(response.estimatedAmountVnd()).isEqualTo(100000L);
        }
        if ("COMPLETE_TICKET".equals(action)) {
            assertThat(response.statusCode()).isEqualTo(GroomingStatus.COMPLETED);
            assertThat(response.invoice()).isNotNull();
            verify(billingHandoffService).createGroomingInvoice(any(ServiceOrder.class), any(Appointment.class), any(GroomingTicket.class), eq(staff));
        }
    }

    private com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse execute(
            String action,
            String mockState,
            UUID petId,
            UUID serviceId,
            OffsetDateTime futureStart,
            UUID ticketId,
            UUID stationId) {
        return switch (action) {
            case "CREATE_BOOKING" -> groomingService.createBooking(new GroomingBookingCreateRequest(
                    petId,
                    serviceId,
                    "PAST_TIME".equals(mockState) ? OffsetDateTime.now().minusHours(1) : futureStart,
                    "Can nhe tay"));
            case "CONFIRM_TICKET" -> groomingService.confirmTicket(ticketId, new GroomingConfirmRequest(stationId, null, "Sap xep station"));
            case "COMPLETE_TICKET" -> groomingService.completeTicket(ticketId, new GroomingCompleteRequest("Da xong"));
            default -> throw new IllegalArgumentException("Unsupported action " + action);
        };
    }

    private GroomingTicket buildTicket(
            UUID ticketId,
            Users owner,
            Pets pet,
            ServiceCatalog service,
            GroomingStatus status) {
        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setId(UUID.randomUUID());
        serviceOrder.setOrderCode("SO-001");
        serviceOrder.setOwner(owner);
        serviceOrder.setPet(pet);
        serviceOrder.setService(service);
        serviceOrder.setStatusCode(status == GroomingStatus.IN_SERVICE ? ServiceOrderStatus.IN_PROGRESS : ServiceOrderStatus.REQUESTED);
        serviceOrder.setBaseAmountVnd(service.getBasePriceVnd());
        serviceOrder.setExtraAmountVnd(0L);
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setServiceOrder(serviceOrder);
        appointment.setAppointmentType(AppointmentType.GROOMING);
        appointment.setScheduledStartAt(OffsetDateTime.now().plusDays(1));
        appointment.setScheduledEndAt(OffsetDateTime.now().plusDays(1).plusMinutes(60));
        appointment.setStatusCode(status == GroomingStatus.IN_SERVICE ? AppointmentStatus.IN_PROGRESS : AppointmentStatus.PENDING);
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(ticketId);
        ticket.setAppointment(appointment);
        ticket.setStatusCode(status);
        ticket.setOwnerNote("Can nhe tay");
        return ticket;
    }

    @org.junit.jupiter.api.Test
    void should_RejectGroomingBooking_When_GroomingSlotFull() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        OffsetDateTime futureStart = OffsetDateTime.now().plusDays(1);
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(petId, serviceId, futureStart, "Need help");

        Users owner = Users.builder().id(ownerId).fullName("Owner").build();
        Pets pet = Pets.builder().id(petId).name("Milu").owner(owner).build();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(serviceId);
        service.setDurationMinutes(60);
        service.setBasePriceVnd(100000L);

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(serviceId, ServiceCategory.GROOMING)).willReturn(Optional.of(service));

        OffsetDateTime expectedEnd = futureStart.plusMinutes(60);

        doThrow(new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL))
                .when(roomAvailabilityChecker).requireGroomingSlotAvailable(futureStart, expectedEnd);

        // WHEN & THEN
        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);

        verify(serviceOrderRepository, never()).save(any());
        verify(appointmentRepository, never()).save(any());
        verify(groomingTicketRepository, never()).save(any());
    }

    @org.junit.jupiter.api.Test
    void should_ListActiveServices() {
        given(serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING))
                .willReturn(List.of(new ServiceCatalog()));
        var res = groomingService.listActiveServices();
        assertThat(res).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_ListActiveStations() {
        given(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc())
                .willReturn(List.of(new GroomingStation()));
        var res = groomingService.listActiveStations();
        assertThat(res).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_ListMyTickets() {
        UUID userId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(userId);
        Page<GroomingTicket> page = new PageImpl<>(List.of(buildTicket(UUID.randomUUID(), new Users(), new Pets(), new ServiceCatalog(), GroomingStatus.PENDING)));
        given(groomingTicketRepository.findByAppointmentServiceOrderOwnerIdOrderByAppointmentScheduledStartAtDesc(eq(userId), any()))
                .willReturn(page);
        var res = groomingService.listMyTickets(PageRequest.of(0, 10));
        assertThat(res.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_GetMyTicket() {
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Users owner = Users.builder().id(userId).build();
        GroomingTicket ticket = buildTicket(ticketId, owner, new Pets(), new ServiceCatalog(), GroomingStatus.PENDING);
        
        given(groomingTicketRepository.findWithDetailsById(ticketId)).willReturn(Optional.of(ticket));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        given(SecurityContextService.getCurrentUserId()).willReturn(userId);

        var res = groomingService.getMyTicket(ticketId);
        assertThat(res).isNotNull();
    }

    @org.junit.jupiter.api.Test
    void should_ListTickets() {
        Page<GroomingTicket> page = new PageImpl<>(List.of(buildTicket(UUID.randomUUID(), new Users(), new Pets(), new ServiceCatalog(), GroomingStatus.PENDING)));
        given(groomingTicketRepository.findByStatusCodeOrderByAppointmentScheduledStartAtAsc(eq(GroomingStatus.PENDING), any()))
                .willReturn(page);
        var res = groomingService.listTickets(GroomingStatus.PENDING, PageRequest.of(0, 10));
        assertThat(res.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_StartTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        GroomingTicket ticket = buildTicket(ticketId, new Users(), new Pets(), new ServiceCatalog(), GroomingStatus.CONFIRMED);
        ticket.setStation(new GroomingStation());
        
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticketId)).willReturn(Optional.of(ticket));
        given(groomingTicketRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = groomingService.startTicket(ticketId);
        assertThat(res.statusCode()).isEqualTo(GroomingStatus.IN_SERVICE);
    }

    @org.junit.jupiter.api.Test
    void should_CancelTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Users owner = Users.builder().id(userId).build();
        GroomingTicket ticket = buildTicket(ticketId, owner, new Pets(), new ServiceCatalog(), GroomingStatus.PENDING);
        
        given(SecurityContextService.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(owner));
        given(groomingTicketRepository.findLockedWithDetailsById(ticketId)).willReturn(Optional.of(ticket));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        given(groomingTicketRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = groomingService.cancelTicket(ticketId, new GroomingCancelRequest("Bận đột xuất"));
        assertThat(res.statusCode()).isEqualTo(GroomingStatus.CANCELLED);
    }

    @org.junit.jupiter.api.Test
    void should_CreateGroomingService() {
        GroomingServiceRequest req = new GroomingServiceRequest("GRM-001", "Spa", "Desc", 100000L, 60);
        given(serviceCatalogRepository.existsByServiceCode("GRM-001")).willReturn(false);
        given(serviceCatalogRepository.save(any())).willAnswer(i -> {
            ServiceCatalog s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        var res = groomingService.createGroomingService(req);
        assertThat(res.serviceCode()).isEqualTo("GRM-001");
    }

    @org.junit.jupiter.api.Test
    void should_UpdateGroomingService() {
        UUID id = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(id);
        service.setCategoryCode(ServiceCategory.GROOMING);
        
        GroomingServiceRequest req = new GroomingServiceRequest("GRM-002", "Spa updated", "Desc", 150000L, 90);
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.existsByServiceCodeAndIdNot("GRM-002", id)).willReturn(false);
        given(serviceCatalogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = groomingService.updateGroomingService(id, req);
        assertThat(res.name()).isEqualTo("Spa updated");
    }

    @org.junit.jupiter.api.Test
    void should_DeactivateGroomingService() {
        UUID id = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(id);
        service.setCategoryCode(ServiceCategory.GROOMING);
        service.setIsActive(true);
        
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(service));

        groomingService.deactivateGroomingService(id);
        verify(serviceCatalogRepository).save(service);
        assertThat(service.getIsActive()).isFalse();
    }

    @org.junit.jupiter.api.Test
    void should_CreateStation() {
        GroomingStationRequest req = new GroomingStationRequest("ST-01", "Station 1", true);
        given(groomingStationRepository.existsByStationCode("ST-01")).willReturn(false);
        given(groomingStationRepository.save(any())).willAnswer(i -> {
            GroomingStation s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        var res = groomingService.createStation(req);
        assertThat(res.stationCode()).isEqualTo("ST-01");
    }

    @org.junit.jupiter.api.Test
    void should_UpdateStation() {
        UUID id = UUID.randomUUID();
        GroomingStation station = new GroomingStation();
        station.setId(id);
        
        GroomingStationRequest req = new GroomingStationRequest("ST-02", "Station 2", false);
        given(groomingStationRepository.findById(id)).willReturn(Optional.of(station));
        given(groomingStationRepository.existsByStationCodeAndIdNot("ST-02", id)).willReturn(false);
        given(groomingStationRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = groomingService.updateStation(id, req);
        assertThat(res.isActive()).isFalse();
    }

    @org.junit.jupiter.api.Test
    void should_DeactivateStation() {
        UUID id = UUID.randomUUID();
        GroomingStation station = new GroomingStation();
        station.setId(id);
        station.setIsActive(true);
        
        given(groomingStationRepository.findById(id)).willReturn(Optional.of(station));

        groomingService.deactivateStation(id);
        verify(groomingStationRepository).save(station);
        assertThat(station.getIsActive()).isFalse();
    }



    @Test
    void createBooking_shouldThrowException_whenPetNotFound() {
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1), "Note"
        );
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(request.petId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_001_NOT_FOUND);
    }

    @Test
    void createBooking_shouldThrowException_whenServiceNotFound() {
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(
                pet.getId(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1), "Note"
        );
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(request.petId())).willReturn(Optional.of(pet));
        given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(request.serviceId(), ServiceCategory.GROOMING))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND);
    }

    @Test
    void confirmTicket_shouldThrowException_whenStationNotFound() {
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        
        GroomingConfirmRequest req = new GroomingConfirmRequest(UUID.randomUUID(), null, "Internal");
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(groomingStationRepository.findWithLockById(req.stationId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> groomingService.confirmTicket(ticket.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND);
    }

    @Test
    void confirmTicket_shouldThrowException_whenStationInactive() {
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        GroomingStation inactiveStation = new GroomingStation();
        inactiveStation.setId(UUID.randomUUID());
        inactiveStation.setIsActive(false);

        GroomingConfirmRequest req = new GroomingConfirmRequest(inactiveStation.getId(), null, "Internal");
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(groomingStationRepository.findWithLockById(req.stationId())).willReturn(Optional.of(inactiveStation));

        assertThatThrownBy(() -> groomingService.confirmTicket(ticket.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND);
    }

    @Test
    void confirmTicket_withAssignedStaff() {
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        
        GroomingStation station = GroomingStation.builder().id(UUID.randomUUID()).isActive(true).build();
        
        ServiceOrder so = new ServiceOrder();
        so.setId(UUID.randomUUID());
        so.setStatusCode(ServiceOrderStatus.REQUESTED);
        so.setOwner(owner);
        so.setPet(pet);
        so.setService(service);
        
        Appointment appt = new Appointment();
        appt.setId(UUID.randomUUID());
        appt.setServiceOrder(so);
        appt.setStatusCode(AppointmentStatus.PENDING);
        appt.setScheduledStartAt(OffsetDateTime.now());
        appt.setScheduledEndAt(OffsetDateTime.now().plusHours(1));
        
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setStatusCode(GroomingStatus.PENDING);
        ticket.setAppointment(appt);
        
        GroomingConfirmRequest req = new GroomingConfirmRequest(station.getId(), staffId, "Internal");
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(groomingStationRepository.findWithLockById(req.stationId())).willReturn(Optional.of(station));
        given(groomingTicketRepository.existsStationConflict(any(), any(), any(), any(), any())).willReturn(false);
        given(groomingTicketRepository.save(any())).willAnswer(i -> i.getArgument(0));

        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse response = groomingService.confirmTicket(ticket.getId(), req);
        assertThat(response).isNotNull();
    }

    @Test
    void completeTicket_whenAlreadyCompleted() {
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setStatusCode(GroomingStatus.COMPLETED);
        
        ServiceOrder so = new ServiceOrder();
        so.setId(UUID.randomUUID());
        so.setOwner(owner);
        so.setPet(pet);
        so.setService(service);
        
        Appointment appt = new Appointment();
        appt.setId(UUID.randomUUID());
        appt.setServiceOrder(so);
        ticket.setAppointment(appt);
        
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        
        GroomingCompleteRequest request = new GroomingCompleteRequest("Done");
        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse response = groomingService.completeTicket(ticket.getId(), request);
        assertThat(response).isNotNull();
    }

    @Test
    void listGroomingServicesForAdmin_shouldReturnList() {
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        given(serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING))
                .willReturn(List.of(service));
        List<GroomingServiceResponse> result = groomingService.listGroomingServicesForAdmin();
        assertThat(result).hasSize(1);
    }

    @Test
    void createGroomingService_success() {
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 30);
        given(serviceCatalogRepository.existsByServiceCode("S1")).willReturn(false);
        given(serviceCatalogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        GroomingServiceResponse result = groomingService.createGroomingService(request);
        assertThat(result).isNotNull();
    }

    @Test
    void createGroomingService_exists() {
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 30);
        given(serviceCatalogRepository.existsByServiceCode("S1")).willReturn(true);

        assertThatThrownBy(() -> groomingService.createGroomingService(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateGroomingService_success() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(serviceId);
        service.setCategoryCode(ServiceCategory.GROOMING);
        
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 30);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.existsByServiceCodeAndIdNot("S1", serviceId)).willReturn(false);
        given(serviceCatalogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        GroomingServiceResponse result = groomingService.updateGroomingService(serviceId, request);
        assertThat(result).isNotNull();
    }

    @Test
    void updateGroomingService_exists() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(serviceId);
        service.setCategoryCode(ServiceCategory.GROOMING);
        
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 30);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.existsByServiceCodeAndIdNot("S1", serviceId)).willReturn(true);

        assertThatThrownBy(() -> groomingService.updateGroomingService(serviceId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateGroomingService_wrongCategory() {
        UUID serviceId = UUID.randomUUID();
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 30);
        ServiceCatalog wrong = new ServiceCatalog();
        wrong.setId(serviceId);
        wrong.setCategoryCode(ServiceCategory.MEDICAL);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(wrong));

        assertThatThrownBy(() -> groomingService.updateGroomingService(serviceId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deactivateGroomingService_success() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(serviceId);
        service.setCategoryCode(ServiceCategory.GROOMING);
        
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(service));
        groomingService.deactivateGroomingService(serviceId);
        verify(serviceCatalogRepository).save(any());
    }

    @Test
    void deactivateGroomingService_wrongCategory() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog wrong = new ServiceCatalog();
        wrong.setId(serviceId);
        wrong.setCategoryCode(ServiceCategory.MEDICAL);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(wrong));

        assertThatThrownBy(() -> groomingService.deactivateGroomingService(serviceId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listStationsForAdmin_success() {
        GroomingStation station = new GroomingStation();
        station.setId(UUID.randomUUID());
        given(groomingStationRepository.findAll()).willReturn(List.of(station));
        List<GroomingStationResponse> result = groomingService.listStationsForAdmin();
        assertThat(result).hasSize(1);
    }

    @Test
    void createStation_success() {
        GroomingStationRequest req = new GroomingStationRequest("ST1", "Station 1", true);
        given(groomingStationRepository.existsByStationCode("ST1")).willReturn(false);
        given(groomingStationRepository.save(any())).willAnswer(i -> i.getArgument(0));
        
        GroomingStationResponse res = groomingService.createStation(req);
        assertThat(res).isNotNull();
    }

    @Test
    void createStation_exists() {
        GroomingStationRequest req = new GroomingStationRequest("ST1", "Station 1", true);
        given(groomingStationRepository.existsByStationCode("ST1")).willReturn(true);
        
        assertThatThrownBy(() -> groomingService.createStation(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStation_success() {
        UUID stationId = UUID.randomUUID();
        GroomingStation station = new GroomingStation();
        station.setId(stationId);
        
        GroomingStationRequest req = new GroomingStationRequest("ST1", "Station 1", true);
        given(groomingStationRepository.findById(stationId)).willReturn(Optional.of(station));
        given(groomingStationRepository.existsByStationCodeAndIdNot("ST1", stationId)).willReturn(false);
        given(groomingStationRepository.save(any())).willAnswer(i -> i.getArgument(0));
        
        GroomingStationResponse res = groomingService.updateStation(stationId, req);
        assertThat(res).isNotNull();
    }

    @Test
    void updateStation_exists() {
        UUID stationId = UUID.randomUUID();
        GroomingStation station = new GroomingStation();
        station.setId(stationId);
        
        GroomingStationRequest req = new GroomingStationRequest("ST1", "Station 1", true);
        given(groomingStationRepository.findById(stationId)).willReturn(Optional.of(station));
        given(groomingStationRepository.existsByStationCodeAndIdNot("ST1", stationId)).willReturn(true);
        
        assertThatThrownBy(() -> groomingService.updateStation(stationId, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deactivateStation_success() {
        UUID stationId = UUID.randomUUID();
        GroomingStation station = new GroomingStation();
        station.setId(stationId);
        
        given(groomingStationRepository.findById(stationId)).willReturn(Optional.of(station));
        groomingService.deactivateStation(stationId);
        verify(groomingStationRepository).save(any());
    }
    
    @Test
    void requireCurrentUserId_null() {
        given(SecurityContextService.getCurrentUserId()).willReturn(null);
        assertThatThrownBy(() -> groomingService.listMyTickets(PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void listTickets_withNullStatus() {
        given(groomingTicketRepository.findAllByOrderByAppointmentScheduledStartAtAsc(any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        groomingService.listTickets(null, org.springframework.data.domain.Pageable.unpaged());
        verify(groomingTicketRepository).findAllByOrderByAppointmentScheduledStartAtAsc(any());
    }

    @Test
    void confirmTicket_withNullAssignedStaff() {
        UUID staffId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        
        GroomingStation station = GroomingStation.builder().id(UUID.randomUUID()).isActive(true).build();
        
        ServiceOrder so = new ServiceOrder();
        so.setId(UUID.randomUUID());
        so.setStatusCode(ServiceOrderStatus.REQUESTED);
        so.setOwner(owner);
        so.setPet(pet);
        so.setService(service);
        
        Appointment appt = new Appointment();
        appt.setId(UUID.randomUUID());
        appt.setServiceOrder(so);
        appt.setStatusCode(AppointmentStatus.PENDING);
        appt.setScheduledStartAt(OffsetDateTime.now());
        appt.setScheduledEndAt(OffsetDateTime.now().plusHours(1));
        
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setStatusCode(GroomingStatus.PENDING);
        ticket.setAppointment(appt);
        
        // request.assignedStaffId() is null here
        GroomingConfirmRequest req = new GroomingConfirmRequest(station.getId(), null, "Internal");
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(groomingTicketRepository.findLockedWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(groomingStationRepository.findWithLockById(req.stationId())).willReturn(Optional.of(station));
        given(groomingTicketRepository.existsStationConflict(any(), any(), any(), any(), any())).willReturn(false);
        given(groomingTicketRepository.save(any())).willAnswer(i -> i.getArgument(0));

        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse response = groomingService.confirmTicket(ticket.getId(), req);
        assertThat(response).isNotNull();
    }

    @Test
    void assertCanAccessTicket_whenNotAdminOrStaff_andNotOwner() {
        UUID currentUserId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        
        ServiceOrder so = new ServiceOrder();
        so.setOwner(owner);
        Appointment appt = new Appointment();
        appt.setServiceOrder(so);
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setAppointment(appt);
        
        given(groomingTicketRepository.findWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        given(SecurityContextService.getCurrentUserId()).willReturn(currentUserId);
        
        assertThatThrownBy(() -> groomingService.getMyTicket(ticket.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }
    
    @Test
    void assertCanAccessTicket_whenAdminOrStaff() {
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        ServiceCatalog service = new ServiceCatalog();
        
        ServiceOrder so = new ServiceOrder();
        so.setOwner(owner);
        so.setPet(pet);
        so.setService(service);
        Appointment appt = new Appointment();
        appt.setServiceOrder(so);
        GroomingTicket ticket = new GroomingTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setAppointment(appt);
        
        given(groomingTicketRepository.findWithDetailsById(ticket.getId())).willReturn(Optional.of(ticket));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        
        com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse response = groomingService.getMyTicket(ticket.getId());
        assertThat(response).isNotNull();
    }

    @Test
    void createBooking_withInvalidStartTime() {
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().minusDays(1), "Note"
        );
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(request.petId()).owner(owner).build();
        
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(request.petId())).willReturn(Optional.of(pet));
        
        ServiceCatalog service = new ServiceCatalog();
        service.setDurationMinutes(30);
        service.setBasePriceVnd(100L);
        given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(request.serviceId(), ServiceCategory.GROOMING))
                .willReturn(Optional.of(service));

        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_003_INVALID_TIME_RANGE);
    }

    @Test
    void createBooking_withInvalidServiceDuration() {
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1), "Note"
        );
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(request.petId()).owner(owner).build();
        
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(request.petId())).willReturn(Optional.of(pet));
        
        ServiceCatalog service = new ServiceCatalog();
        service.setDurationMinutes(0); // Invalid
        service.setBasePriceVnd(100L);
        given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(request.serviceId(), ServiceCategory.GROOMING))
                .willReturn(Optional.of(service));

        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND);
    }

    @Test
    void createBooking_withInvalidServicePrice() {
        GroomingBookingCreateRequest request = new GroomingBookingCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(1), "Note"
        );
        UUID ownerId = UUID.randomUUID();
        Users owner = Users.builder().id(ownerId).build();
        Pets pet = Pets.builder().id(request.petId()).owner(owner).build();
        
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(request.petId())).willReturn(Optional.of(pet));
        
        ServiceCatalog service = new ServiceCatalog();
        service.setDurationMinutes(30);
        service.setBasePriceVnd(-100L); // Invalid
        given(serviceCatalogRepository.findByIdAndCategoryCodeAndIsActiveTrue(request.serviceId(), ServiceCategory.GROOMING))
                .willReturn(Optional.of(service));

        assertThatThrownBy(() -> groomingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND);
    }

    @Test
    void createGroomingService_withInvalidRequestDuration() {
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", 100L, 0); // Invalid duration
        assertThatThrownBy(() -> groomingService.createGroomingService(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }
    
    @Test
    void createGroomingService_withInvalidRequestPrice() {
        GroomingServiceRequest request = new GroomingServiceRequest("S1", "Name", "Desc", -100L, 30); // Invalid price
        assertThatThrownBy(() -> groomingService.createGroomingService(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

}