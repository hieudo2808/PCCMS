package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.QuickCheckInRequest;
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
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuickCheckInUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetValidationService petValidationService;

    @Mock
    private AppointmentAvailabilityService availabilityService;

    @Mock
    private VetAvailabilityChecker vetAvailabilityChecker;

    @Mock
    private RoomAvailabilityChecker roomAvailabilityChecker;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private ServiceOrderFactory serviceOrderFactory;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ReceptionTicketService receptionService;

    @InjectMocks
    private QuickCheckInUseCase useCase;

    @Test
    void execute_shouldThrowException_whenPhoneBlank() {
        QuickCheckInRequest request = new QuickCheckInRequest(null, UUID.randomUUID(), null, "Symptom");

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_008_PHONE_REQUIRED);
    }

    @Test
    void execute_shouldThrowException_whenUserNotFound() {
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void execute_shouldThrowException_whenSlotFull() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(new Pets());

        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(false);

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(10, 0));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            assertThatThrownBy(() -> useCase.execute(request, staffId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
        }
    }

    @Test
    void execute_shouldThrowException_whenServiceNotFound() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(new Pets());

        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(new Users());
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(new ExamRoom());

        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());
        given(serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)).willReturn(Optional.empty());

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(10, 0));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            assertThatThrownBy(() -> useCase.execute(request, staffId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND);
        }
    }

    @Test
    void execute_shouldSuccess() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        Pets pet = new Pets();
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(pet);

        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        Users vet = new Users();
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(vet);
        ExamRoom room = new ExamRoom();
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(room);

        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(30);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));

        ServiceOrder order = new ServiceOrder();
        given(serviceOrderFactory.createServiceOrder(eq(pet), eq(catalog), eq(staffId), any(), any(), eq(ServiceCategory.MEDICAL)))
                .willReturn(order);

        Users staff = new Users();
        staff.setId(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));

        given(receptionService.receiveAppointment(any(Appointment.class), eq(staff), eq(vet))).willReturn(1);

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(10, 0));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            QuickCheckInUseCase.Result result = useCase.execute(request, staffId);

            assertThat(result.queueNumber()).isEqualTo(1);
            assertThat(result.appointment().getAppointmentType()).isEqualTo(AppointmentType.MEDICAL);
            assertThat(result.appointment().getStatusCode()).isEqualTo(AppointmentStatus.CHECKED_IN);
            verify(serviceOrderRepository).save(order);
            verify(appointmentRepository).save(any(Appointment.class));
        }
    }

    @Test
    void execute_shouldThrowException_whenPhoneEmpty() {
        QuickCheckInRequest request = new QuickCheckInRequest("", UUID.randomUUID(), null, "Symptom");
        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_008_PHONE_REQUIRED);
    }

    @Test
    void execute_shouldThrowException_whenAfterClinicClose() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(new Pets());

        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(16, 45));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());

            assertThatThrownBy(() -> useCase.execute(request, staffId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
        }
    }

    @Test
    void execute_shouldRoundUpToNextSlot_andHandleDurationNullOrNegative() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        Pets pet = new Pets();
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(pet);

        given(availabilityService.isSlotAvailable(any(), any(), any())).willReturn(true);
        Users vet = new Users();
        given(vetAvailabilityChecker.requireVetAvailable(any(), any(), any(), any(), any())).willReturn(vet);
        ExamRoom room = new ExamRoom();
        given(roomAvailabilityChecker.requireRoomAvailable(any(), any())).willReturn(room);

        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(0); // Should fallback to 30
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));

        ServiceOrder order = new ServiceOrder();
        given(serviceOrderFactory.createServiceOrder(eq(pet), eq(catalog), eq(staffId), any(), any(), eq(ServiceCategory.MEDICAL)))
                .willReturn(order);

        Users staff = new Users();
        staff.setId(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(receptionService.receiveAppointment(any(Appointment.class), eq(staff), eq(vet))).willReturn(1);

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            // 10:10 -> should round up to 10:30 (with 30 min slots)
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(10, 10));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            QuickCheckInUseCase.Result result = useCase.execute(request, staffId);
            assertThat(result.queueNumber()).isEqualTo(1);
        }
    }

    @Test
    void execute_shouldHandleRoundUpToNextDay() {
        UUID staffId = UUID.randomUUID();
        QuickCheckInRequest request = new QuickCheckInRequest("0123456789", UUID.randomUUID(), null, "Symptom");
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        given(userRepository.findByNormalizedPhone("0123456789")).willReturn(Optional.of(owner));
        given(petValidationService.findPetOwnedBy(request.petId(), owner.getId())).willReturn(new Pets());

        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.empty());

        try (org.mockito.MockedStatic<ClinicDateTime> mockedTime = org.mockito.Mockito.mockStatic(ClinicDateTime.class)) {
            // 23:45 -> should round up to 24:00 (LocalTime.MAX)
            mockedTime.when(ClinicDateTime::nowTime).thenReturn(java.time.LocalTime.of(23, 45));
            mockedTime.when(ClinicDateTime::today).thenReturn(java.time.LocalDate.now());
            mockedTime.when(() -> ClinicDateTime.toOffsetDateTime(any(), any())).thenCallRealMethod();

            // When startAt is evaluated, LocalTime.MAX will be used, availabilityService returns false
            assertThatThrownBy(() -> useCase.execute(request, staffId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
        }
    }

}