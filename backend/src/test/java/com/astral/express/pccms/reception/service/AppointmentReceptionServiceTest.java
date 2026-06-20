package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.BusinessNotificationService;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import com.astral.express.pccms.reception.repository.AppointmentReceptionCommandRepository;
import com.astral.express.pccms.reception.repository.AppointmentReceptionQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentReceptionServiceTest {
    @Mock
    private BusinessNotificationService businessNotificationService;

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private AppointmentReceptionQueryRepository appointmentReceptionQueryRepository;
    @Mock
    private AppointmentReceptionCommandRepository appointmentReceptionCommandRepository;

    @InjectMocks
    private AppointmentReceptionService service;

    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
    }

    @Test
    
    void listAppointments_shouldReturnList() {
        AppointmentReceptionResponse response = new AppointmentReceptionResponse(
                appointmentId, "PENDING", Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().plusHours(1)), "Fever", "SO-123",
                "John", null, "Rex", null, null);
        given(appointmentReceptionQueryRepository.listAppointments("John", "PENDING")).willReturn(List.of(response));

        List<AppointmentReceptionResponse> responses = service.listAppointments("John", "PENDING");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(appointmentId);
    }

    @Test
    void quickCreateAndReceive_shouldCreateUserAndPetAndOrderAndAppointment() {
        QuickAppointmentRequest req = new QuickAppointmentRequest("0123456789", "John", "Rex", UUID.randomUUID(), "MED-GENERAL", "2026-06-12T10:00:00Z", "2026-06-12T10:30:00Z", "Fever", "Note");

        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID newApptId = UUID.randomUUID();

        given(appointmentReceptionCommandRepository.findOwnerIdByPhone("0123456789")).willReturn(Optional.empty());
        given(appointmentReceptionCommandRepository.createWalkinOwner(anyString(), eq("0123456789"), eq("John"))).willReturn(ownerId);
        given(appointmentReceptionCommandRepository.findPetId(ownerId, "Rex")).willReturn(Optional.empty());
        given(appointmentReceptionCommandRepository.createPet(ownerId, "Rex")).willReturn(petId);

        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(appointmentReceptionCommandRepository.createServiceOrder(anyString(), eq(ownerId), eq(petId), any(), any(), any(), anyString())).willReturn(orderId);

        given(appointmentReceptionCommandRepository.createAppointment(eq(orderId), any(), any(), any(), eq("Fever"), eq("Note"), any())).willReturn(newApptId);

        // For receive()
        given(appointmentReceptionCommandRepository.findAppointmentStatus(newApptId)).willReturn(Optional.of("PENDING"));

        // For getById()
        given(appointmentReceptionQueryRepository.findById(newApptId)).willReturn(Optional.of(new AppointmentReceptionResponse(newApptId, "CHECKED_IN", null, null, null, "SO-123", "John", null, null, null, null)));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

    @Test
    void receive_shouldThrowException_whenNotFound() {
        given(appointmentReceptionCommandRepository.findAppointmentStatus(appointmentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.receive(appointmentId, new AppointmentReceiveRequest(null, "note")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND);
    }

    @Test
    void receive_shouldThrowException_whenCancelled() {
        given(appointmentReceptionCommandRepository.findAppointmentStatus(appointmentId)).willReturn(Optional.of("CANCELLED"));

        assertThatThrownBy(() -> service.receive(appointmentId, new AppointmentReceiveRequest(null, "note")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE);
    }

    @Test
    void cancel_shouldUpdateStatusAndReturn() {
        given(appointmentReceptionQueryRepository.findById(appointmentId)).willReturn(Optional.of(new AppointmentReceptionResponse(appointmentId, "CANCELLED", null, null, null, "SO-123", "John", null, null, null, null)));

        AppointmentReceptionResponse res = service.cancel(appointmentId, new AppointmentCancelRequest("reason"));

        assertThat(res.statusCode()).isEqualTo("CANCELLED");
        verify(appointmentReceptionCommandRepository).cancelAppointment(appointmentId, "reason");
    }

    @Test
    void listAppointments_shouldReturnList_WhenKeywordIsNullAndStatusIsEmpty() {
        given(appointmentReceptionQueryRepository.listAppointments(null, " ")).willReturn(List.of(new AppointmentReceptionResponse(appointmentId, "PENDING", null, null, null, null, null, null, null, null, null)));

        List<AppointmentReceptionResponse> responses = service.listAppointments(null, " ");

        assertThat(responses).hasSize(1);
    }

    @Test
    void quickCreateAndReceive_shouldHandleNullDatesAndNullServiceCodeAndReturnResponse() {
        QuickAppointmentRequest req = new QuickAppointmentRequest("0123456789", "John", "Rex", UUID.randomUUID(), " ", null, " ", "Fever", "Note");

        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID newApptId = UUID.randomUUID();

        given(appointmentReceptionCommandRepository.findOwnerIdByPhone("0123456789")).willReturn(Optional.of(ownerId));
        given(appointmentReceptionCommandRepository.findPetId(ownerId, "Rex")).willReturn(Optional.of(petId));

        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(appointmentReceptionCommandRepository.createServiceOrder(anyString(), eq(ownerId), eq(petId), any(), any(), any(), eq("MED-GENERAL"))).willReturn(orderId);

        given(appointmentReceptionCommandRepository.createAppointment(eq(orderId), any(), any(), any(), eq("Fever"), eq("Note"), any())).willReturn(newApptId);

        given(appointmentReceptionCommandRepository.findAppointmentStatus(newApptId)).willReturn(Optional.of("PENDING"));

        given(appointmentReceptionQueryRepository.findById(newApptId)).willReturn(Optional.of(new AppointmentReceptionResponse(newApptId, "CHECKED_IN", null, null, null, "SO-123", "John", null, null, null, null)));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

    @Test
    void receive_shouldWork_WhenRequestIsNull() {
        given(appointmentReceptionCommandRepository.findAppointmentStatus(appointmentId)).willReturn(Optional.of("CONFIRMED"));
        given(appointmentReceptionQueryRepository.findById(appointmentId)).willReturn(Optional.of(new AppointmentReceptionResponse(appointmentId, "CHECKED_IN", null, null, null, "SO-123", "John", null, null, null, null)));

        AppointmentReceptionResponse res = service.receive(appointmentId, null);
        assertThat(res.statusCode()).isEqualTo("CHECKED_IN");
    }

    @Test
    void cancel_shouldWork_WhenRequestIsNull() {
        given(appointmentReceptionQueryRepository.findById(appointmentId)).willReturn(Optional.of(new AppointmentReceptionResponse(appointmentId, "CANCELLED", null, null, null, "SO-123", "John", null, null, null, null)));

        AppointmentReceptionResponse res = service.cancel(appointmentId, null);
        assertThat(res.statusCode()).isEqualTo("CANCELLED");
    }

    @Test
    void quickCreateAndReceive_shouldHandleLocalDateTime() {
        QuickAppointmentRequest req = new QuickAppointmentRequest("0123456789", "John", "Rex", UUID.randomUUID(), " ", "2026-06-12T10:00:00", "2026-06-12", "Fever", "Note");

        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID newApptId = UUID.randomUUID();

        given(appointmentReceptionCommandRepository.findOwnerIdByPhone("0123456789")).willReturn(Optional.of(ownerId));
        given(appointmentReceptionCommandRepository.findPetId(ownerId, "Rex")).willReturn(Optional.of(petId));
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(appointmentReceptionCommandRepository.createServiceOrder(anyString(), any(), any(), any(), any(), any(), anyString())).willReturn(orderId);
        given(appointmentReceptionCommandRepository.createAppointment(any(), any(), any(), any(), any(), any(), any())).willReturn(newApptId);
        given(appointmentReceptionCommandRepository.findAppointmentStatus(newApptId)).willReturn(Optional.of("PENDING"));
        given(appointmentReceptionQueryRepository.findById(newApptId)).willReturn(Optional.of(new AppointmentReceptionResponse(newApptId, "CHECKED_IN", null, null, null, null, null, null, null, null, null)));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

}
