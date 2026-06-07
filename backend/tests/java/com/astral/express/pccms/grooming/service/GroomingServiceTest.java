package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.boarding.entity.ServiceCatalog;
import com.astral.express.pccms.boarding.entity.ServiceCategory;
import com.astral.express.pccms.boarding.entity.ServiceOrder;
import com.astral.express.pccms.boarding.entity.ServiceOrderStatus;
import com.astral.express.pccms.boarding.repository.ServiceCatalogRepository;
import com.astral.express.pccms.boarding.repository.ServiceOrderRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.grooming.entity.Appointment;
import com.astral.express.pccms.grooming.entity.AppointmentStatus;
import com.astral.express.pccms.grooming.entity.AppointmentType;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.entity.GroomingStatus;
import com.astral.express.pccms.grooming.entity.GroomingTicket;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.grooming.repository.AppointmentRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.grooming.repository.GroomingTicketRepository;
import com.astral.express.pccms.grooming.service.impl.GroomingServiceImpl;
import com.astral.express.pccms.identity.security.SecurityHelper;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class GroomingServiceTest {

    @Mock
    private SecurityHelper securityHelper;

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

    private GroomingServiceImpl groomingService;

    @BeforeEach
    void setUp() {
        groomingService = new GroomingServiceImpl(
                securityHelper,
                userRepository,
                petRepository,
                serviceCatalogRepository,
                serviceOrderRepository,
                appointmentRepository,
                groomingTicketRepository,
                groomingStationRepository,
                billingHandoffService,
                invoiceRepository,
                new GroomingMapper());
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
        ServiceCatalog service = ServiceCatalog.builder()
                .id(serviceId)
                .serviceCode("GRM-BATH")
                .name("Tam say")
                .categoryCode(ServiceCategory.GROOMING)
                .basePriceVnd(100000L)
                .durationMinutes(60)
                .isActive(true)
                .build();

        if ("CREATE_BOOKING".equals(action)) {
            given(securityHelper.getCurrentUserId()).willReturn(ownerId);
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
            given(securityHelper.getCurrentUserId()).willReturn(staff.getId());
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
            given(securityHelper.getCurrentUserId()).willReturn(staff.getId());
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
        ServiceOrder serviceOrder = ServiceOrder.builder()
                .id(UUID.randomUUID())
                .orderCode("SO-001")
                .owner(owner)
                .pet(pet)
                .service(service)
                .statusCode(status == GroomingStatus.IN_SERVICE ? ServiceOrderStatus.IN_PROGRESS : ServiceOrderStatus.REQUESTED)
                .baseAmountVnd(service.getBasePriceVnd())
                .extraAmountVnd(0L)
                .build();
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .serviceOrder(serviceOrder)
                .appointmentType(AppointmentType.GROOMING)
                .scheduledStartAt(OffsetDateTime.now().plusDays(1))
                .scheduledEndAt(OffsetDateTime.now().plusDays(1).plusMinutes(60))
                .statusCode(status == GroomingStatus.IN_SERVICE ? AppointmentStatus.IN_PROGRESS : AppointmentStatus.PENDING)
                .build();
        return GroomingTicket.builder()
                .id(ticketId)
                .appointment(appointment)
                .statusCode(status)
                .ownerNote("Can nhe tay")
                .build();
    }
}
