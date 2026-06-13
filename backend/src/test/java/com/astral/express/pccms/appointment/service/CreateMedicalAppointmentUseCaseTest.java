package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateMedicalAppointmentUseCaseTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentAvailabilityService availabilityService;

    @Mock
    private VetAvailabilityChecker vetAvailabilityChecker;

    @Mock
    private RoomAvailabilityChecker roomAvailabilityChecker;

    @Mock
    private ServiceOrderFactory serviceOrderFactory;

    @Mock
    private PetValidationService petValidationService;

    @InjectMocks
    private CreateMedicalAppointmentUseCase useCase;

    @Test
    void createMedicalAppointment_shouldThrowException_whenDateIsPast() {
        UUID ownerId = UUID.randomUUID();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().minusDays(1), LocalTime.of(10, 0), UUID.randomUUID(), "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(new Pets());

        assertThatThrownBy(() -> useCase.createMedicalAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_002_PAST_DATETIME);
    }

    @Test
    void createMedicalAppointment_shouldThrowException_whenSlotIsPast() {
        UUID ownerId = UUID.randomUUID();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now(), LocalTime.now().minusHours(5), UUID.randomUUID(), "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(new Pets());
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL"))
                .willReturn(Optional.of(serviceCatalog(30)));

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            mockedTime.when(ClinicDateTime::now).thenReturn(OffsetDateTime.now());
            mockedTime.when(ClinicDateTime::today).thenReturn(LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            assertThatThrownBy(() -> useCase.createMedicalAppointment(request, ownerId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
    }

    @Test
    void createMedicalAppointment_shouldThrowException_whenSlotFull() {
        UUID ownerId = UUID.randomUUID();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), UUID.randomUUID(), "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(new Pets());
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());
        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(false);

        assertThatThrownBy(() -> useCase.createMedicalAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
    }

    @Test
    void createMedicalAppointment_shouldThrowException_whenServiceNotFound() {
        UUID ownerId = UUID.randomUUID();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), UUID.randomUUID(), "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(new Pets());
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());
        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(new Users());
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(new ExamRoom());
        given(serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createMedicalAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND);
    }

    @Test
    void createMedicalAppointment_shouldCreateAppointment() {
        UUID ownerId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Pets pet = new Pets();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), vetId, "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(pet);
        ServiceCatalog service = serviceCatalog(30);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(service));
        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        Users vet = new Users();
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(vet);
        ExamRoom room = new ExamRoom();
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(room);

        ServiceOrder order = new ServiceOrder();
        given(serviceOrderFactory.createServiceOrder(eq(pet), eq(service), eq(ownerId), any(), any(), eq(ServiceCategory.MEDICAL)))
                .willReturn(order);

        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        appointment.setStatusCode(AppointmentStatus.PENDING);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(appointment);

        Appointment result = useCase.createMedicalAppointment(request, ownerId);

        assertThat(result.getAppointmentType()).isEqualTo(AppointmentType.MEDICAL);
        assertThat(result.getStatusCode()).isEqualTo(AppointmentStatus.PENDING);
        verify(serviceOrderRepository).save(order);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    private ServiceCatalog serviceCatalog(int duration) {
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(duration);
        return catalog;
    }

    @Test
    void createMedicalAppointment_shouldUseFallbackService() {
        UUID ownerId = UUID.randomUUID();
        Pets pet = new Pets();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(pet);
        
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());
        ServiceCatalog fallbackService = serviceCatalog(0); // 0 should trigger default slot duration 30
        given(serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)).willReturn(Optional.of(fallbackService));
        
        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        Users vet = new Users();
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(vet);
        ExamRoom room = new ExamRoom();
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(room);

        ServiceOrder order = new ServiceOrder();
        given(serviceOrderFactory.createServiceOrder(eq(pet), eq(fallbackService), eq(ownerId), any(), any(), eq(ServiceCategory.MEDICAL)))
                .willReturn(order);

        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        appointment.setStatusCode(AppointmentStatus.PENDING);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(appointment);

        Appointment result = useCase.createMedicalAppointment(request, ownerId);

        assertThat(result.getAppointmentType()).isEqualTo(AppointmentType.MEDICAL);
        // When request vet is null, requestedStaff is null
        verify(appointmentRepository).save(org.mockito.ArgumentMatchers.argThat(a -> a.getRequestedStaff() == null));
    }
    
    @Test
    void createMedicalAppointment_shouldHandleNullDuration() {
        UUID ownerId = UUID.randomUUID();
        Pets pet = new Pets();
        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(10, 0), null, "Symptom", "Note"
        );
        given(petValidationService.findPetOwnedBy(request.petId(), ownerId)).willReturn(pet);
        
        ServiceCatalog service = new ServiceCatalog();
        service.setDurationMinutes(null);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(service));
        
        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(new Users());
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(new ExamRoom());

        ServiceOrder order = new ServiceOrder();
        given(serviceOrderFactory.createServiceOrder(eq(pet), eq(service), eq(ownerId), any(), any(), eq(ServiceCategory.MEDICAL)))
                .willReturn(order);

        Appointment appointment = new Appointment();
        given(appointmentRepository.save(any(Appointment.class))).willReturn(appointment);

        Appointment result = useCase.createMedicalAppointment(request, ownerId);
        assertThat(result).isNotNull();
    }

}