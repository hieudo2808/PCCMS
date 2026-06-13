package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.CustomerLookupResponse;
import com.astral.express.pccms.appointment.dto.response.QueueEntryResponse;
import com.astral.express.pccms.appointment.dto.response.ServiceCatalogOptionResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ReceptionTicket;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppointmentQueryUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentResponseAssembler assembler;

    @Mock
    private ReceptionTicketService receptionService;

    @InjectMocks
    private AppointmentQueryUseCase useCase;

    @Test
    void getAppointmentById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAppointmentById(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_001_NOT_FOUND);
    }

    @Test
    void getAppointmentById_shouldReturnResponse() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(receptionService.getQueueNumberForAppointment(id)).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        AppointmentResponse response = useCase.getAppointmentById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void listOwnerAppointments_shouldReturnPage() {
        UUID ownerId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        given(appointmentRepository.findByOwnerId(ownerId, pageable)).willReturn(new PageImpl<>(List.of(appointment), pageable, 1));
        given(receptionService.getQueueNumberForAppointment(appointment.getId())).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(appointment.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        PageResponse<AppointmentResponse> response = useCase.listOwnerAppointments(ownerId, pageable);

        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void listTodayAppointments_shouldReturnFilteredList() {
        LocalDate date = LocalDate.now();
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatusCode(AppointmentStatus.PENDING);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setPhone("1234567890");
        owner.setFullName("John Doe");
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findAppointmentsForDay(any(), any())).willReturn(List.of(appointment));
        given(receptionService.getQueueNumberForAppointment(appointment.getId())).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(appointment.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        List<AppointmentResponse> response = useCase.listTodayAppointments(date, AppointmentStatus.PENDING, "123", "john");

        assertThat(response).hasSize(1);
    }

    @Test
    void lookupCustomerByPhone_shouldThrowException_whenPhoneNull() {
        assertThatThrownBy(() -> useCase.lookupCustomerByPhone(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_008_PHONE_REQUIRED);
    }

    @Test
    void lookupCustomerByPhone_shouldThrowException_whenUserNotFound() {
        given(userRepository.findByNormalizedPhone("123456789")).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.lookupCustomerByPhone("123-456-789"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void lookupCustomerByPhone_shouldReturnCustomer() {
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        owner.setPhone("123456789");
        given(userRepository.findByNormalizedPhone("123456789")).willReturn(Optional.of(owner));

        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setName("Rex");
        given(petRepository.findByOwner_IdAndIsActive(eq(owner.getId()), eq(true), any())).willReturn(new PageImpl<>(List.of(pet)));

        CustomerLookupResponse response = useCase.lookupCustomerByPhone("123-456-789");

        assertThat(response.ownerId()).isEqualTo(owner.getId());
        assertThat(response.pets()).hasSize(1);
        assertThat(response.pets().get(0).name()).isEqualTo("Rex");
    }

    @Test
    void getVetQueue_shouldReturnQueue() {
        UUID vetId = UUID.randomUUID();
        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setQueueNumber(1);
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        ServiceOrder order = new ServiceOrder();
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setName("Rex");
        order.setPet(pet);
        Users owner = new Users();
        owner.setFullName("John Doe");
        order.setOwner(owner);
        appointment.setServiceOrder(order);
        ticket.setAppointment(appointment);

        given(receptionService.getQueueForVet(eq(vetId), any(), any())).willReturn(List.of(ticket));

        List<QueueEntryResponse> queue = useCase.getVetQueue(vetId, LocalDate.now());

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).queueNumber()).isEqualTo(1);
    }

    @Test
    void listServicesByCategory_shouldReturnOptions() {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setId(UUID.randomUUID());
        catalog.setName("Service");
        given(serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.MEDICAL)).willReturn(List.of(catalog));
        given(assembler.toServiceCatalogOptionResponse(catalog)).willReturn(new ServiceCatalogOptionResponse(catalog.getId(), "code", "Service", null, null, null));

        List<ServiceCatalogOptionResponse> options = useCase.listServicesByCategory(ServiceCategory.MEDICAL);

        assertThat(options).hasSize(1);
    }

    @Test
    void listTodayAppointments_shouldReturnFilteredList_withNullFilters() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatusCode(AppointmentStatus.PENDING);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setPhone("1234567890");
        owner.setFullName("John Doe");
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findAppointmentsForDay(any(), any())).willReturn(List.of(appointment));
        given(receptionService.getQueueNumberForAppointment(appointment.getId())).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(appointment.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        // Testing with null date, null status, null/blank phone, null/blank name
        List<AppointmentResponse> response = useCase.listTodayAppointments(null, null, null, null);
        assertThat(response).hasSize(1);
        
        List<AppointmentResponse> response2 = useCase.listTodayAppointments(null, null, " ", " ");
        assertThat(response2).hasSize(1);
    }
    
    @Test
    void listTodayAppointments_shouldFilterOut_whenNotMatching() {
        LocalDate date = LocalDate.now();
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatusCode(AppointmentStatus.PENDING);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setPhone("1234567890");
        owner.setFullName("John Doe");
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findAppointmentsForDay(any(), any())).willReturn(List.of(appointment));

        // Mismatched status
        List<AppointmentResponse> responseStatus = useCase.listTodayAppointments(date, AppointmentStatus.COMPLETED, null, null);
        assertThat(responseStatus).isEmpty();
        
        // Mismatched phone
        List<AppointmentResponse> responsePhone = useCase.listTodayAppointments(date, null, "999", null);
        assertThat(responsePhone).isEmpty();
        
        // Mismatched name
        List<AppointmentResponse> responseName = useCase.listTodayAppointments(date, null, null, "Jane");
        assertThat(responseName).isEmpty();
    }

    @Test
    void getVetQueue_shouldReturnQueue_withNullDate() {
        UUID vetId = UUID.randomUUID();
        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setQueueNumber(1);
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        ServiceOrder order = new ServiceOrder();
        Pets pet = new Pets();
        pet.setId(UUID.randomUUID());
        pet.setName("Rex");
        order.setPet(pet);
        Users owner = new Users();
        owner.setFullName("John Doe");
        order.setOwner(owner);
        appointment.setServiceOrder(order);
        ticket.setAppointment(appointment);

        given(receptionService.getQueueForVet(eq(vetId), any(), any())).willReturn(List.of(ticket));

        List<QueueEntryResponse> queue = useCase.getVetQueue(vetId, null);

        assertThat(queue).hasSize(1);
    }

}