package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AppointmentLifecycleUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentResponseAssembler assembler;

    @Mock
    private ReceptionTicketService receptionService;

    @InjectMocks
    private AppointmentLifecycleUseCase useCase;

    @Test
    void checkIn_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.checkIn(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_001_NOT_FOUND);
    }

    @Test
    void checkIn_shouldThrowException_whenCancelled() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.CANCELLED);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.checkIn(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_003_ALREADY_CANCELLED);
    }

    @Test
    void checkIn_shouldThrowException_whenAlreadyCheckedIn() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.CHECKED_IN);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.checkIn(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_004_ALREADY_CHECKED_IN);
    }

    @Test
    void checkIn_shouldThrowException_whenStaffNotFound() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.PENDING);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(userRepository.findById(staffId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.checkIn(id, staffId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void checkIn_shouldThrowException_whenVetNotFound() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.PENDING);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(userRepository.findById(staffId)).willReturn(Optional.of(new Users()));

        assertThatThrownBy(() -> useCase.checkIn(id, staffId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
    }

    @Test
    void checkIn_shouldSuccess() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.PENDING);
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        appointment.setAssignedStaff(vet);
        ServiceOrder order = new ServiceOrder();
        appointment.setServiceOrder(order);

        Users staff = new Users();
        staff.setId(staffId);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(receptionService.receiveAppointment(appointment, staff, vet)).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        AppointmentResponse response = useCase.checkIn(id, staffId);

        assertThat(response.id()).isEqualTo(id);
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.CHECKED_IN);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.CONFIRMED);
        assertThat(order.getUpdatedBy()).isEqualTo(staffId);
    }

    @Test
    void startExam_shouldThrowException_whenNotMedical() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.GROOMING);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.startExam(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void startExam_shouldThrowException_whenVetNotAssigned() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.startExam(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
    }

    @Test
    void startExam_shouldThrowException_whenWrongVet() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        appointment.setAssignedStaff(vet);
        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.startExam(id, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void startExam_shouldReturnResponse_whenAlreadyStarted() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.IN_PROGRESS);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(receptionService.getQueueNumberForAppointment(id)).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        AppointmentResponse response = useCase.startExam(id, vetId);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void startExam_shouldThrowException_whenNotCheckedIn() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.PENDING);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.startExam(id, vetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void startExam_shouldSuccess() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.CHECKED_IN);
        ServiceOrder order = new ServiceOrder();
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(receptionService.getQueueNumberForAppointment(id)).willReturn(1);
        given(assembler.toResponse(appointment, 1)).willReturn(new AppointmentResponse(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        AppointmentResponse response = useCase.startExam(id, vetId);

        assertThat(response.id()).isEqualTo(id);
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(order.getActualStartAt()).isNotNull();
        assertThat(order.getUpdatedBy()).isEqualTo(vetId);
    }

    @Test
    void completeMedicalAppointment_shouldDoNothing_whenIdNull() {
        useCase.completeMedicalAppointment(null, UUID.randomUUID());
    }

    @Test
    void completeMedicalAppointment_shouldDoNothing_whenAlreadyCompleted() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.COMPLETED);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        useCase.completeMedicalAppointment(id, vetId);
    }

    @Test
    void completeMedicalAppointment_shouldThrowException_whenPending() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.PENDING);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.completeMedicalAppointment(id, vetId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void completeMedicalAppointment_shouldSuccess() {
        UUID id = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        Users vet = new Users();
        vet.setId(vetId);
        appointment.setAssignedStaff(vet);
        appointment.setStatusCode(AppointmentStatus.IN_PROGRESS);
        ServiceOrder order = new ServiceOrder();
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        useCase.completeMedicalAppointment(id, vetId);

        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    void cancel_shouldThrowException_whenNotOwnerAndNotStaff() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setId(UUID.randomUUID());
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.cancel(id, actorId, false))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyCancelled() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.CANCELLED);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setId(actorId);
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.cancel(id, actorId, false))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_003_ALREADY_CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenNotPendingAndNotStaff() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.CONFIRMED);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setId(actorId);
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.cancel(id, actorId, false))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_007_CANNOT_CANCEL);
    }

    @Test
    void cancel_shouldThrowException_whenInProgressAndStaff() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.IN_PROGRESS);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> useCase.cancel(id, actorId, true))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_007_CANNOT_CANCEL);
    }

    @Test
    void cancel_shouldSuccess() {
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setStatusCode(AppointmentStatus.PENDING);
        ServiceOrder order = new ServiceOrder();
        Users owner = new Users();
        owner.setId(actorId);
        order.setOwner(owner);
        appointment.setServiceOrder(order);

        given(appointmentRepository.findDetailById(id)).willReturn(Optional.of(appointment));
        given(receptionService.getQueueNumberForAppointment(id)).willReturn(null);
        given(assembler.toResponse(appointment, null)).willReturn(new AppointmentResponse(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        AppointmentResponse response = useCase.cancel(id, actorId, false);

        assertThat(response.id()).isEqualTo(id);
        assertThat(appointment.getStatusCode()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }
}
