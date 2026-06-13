package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
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
    private JdbcTemplate jdbc;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private AppointmentReceptionService service;

    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
    }

    @Test
    
    void listAppointments_shouldReturnList() {
        Map<String, Object> row = Map.of(
                "id", appointmentId,
                "status_code", "PENDING",
                "scheduled_start_at", Timestamp.valueOf(LocalDateTime.now()),
                "scheduled_end_at", Timestamp.valueOf(LocalDateTime.now().plusHours(1)),
                "symptom_text", "Fever",
                "order_code", "SO-123",
                "owner_name", "John",
                "pet_name", "Rex"
        );
        given(jdbc.queryForList(anyString(), any(Object[].class))).willReturn(List.of(row));

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

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM users WHERE phone = ?"), eq("0123456789")))
                .willThrow(new EmptyResultDataAccessException(1));
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO users"), any(), eq("0123456789"), eq("John")))
                .willReturn(Map.of("id", ownerId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM pets WHERE owner_id = ?"), eq(ownerId), eq("Rex")))
                .willThrow(new EmptyResultDataAccessException(1));
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO pets"), eq(ownerId), eq("Rex")))
                .willReturn(Map.of("id", petId));

        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO service_orders"), any(), eq(ownerId), eq(petId), any(), any(), any(), any())).willReturn(Map.of("id", orderId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO appointments"), eq(orderId), any(), any(), any(), eq("Fever"), eq("Note"), any())).willReturn(Map.of("id", newApptId));

        // For receive()
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code FROM appointments WHERE id = ?"), eq(newApptId))).willReturn(Map.of("status_code", "PENDING"));
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), any(), eq(newApptId))).willReturn(Map.of("id", newApptId));

        // For getById()
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(newApptId))).willReturn(Map.of("id", newApptId, "status_code", "CHECKED_IN", "order_code", "SO-123", "owner_name", "John"));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

    @Test
    void receive_shouldThrowException_whenNotFound() {
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code"), any())).willThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> service.receive(appointmentId, new AppointmentReceiveRequest(null, "note")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND);
    }

    @Test
    void receive_shouldThrowException_whenCancelled() {
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "CANCELLED"));

        assertThatThrownBy(() -> service.receive(appointmentId, new AppointmentReceiveRequest(null, "note")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE);
    }

    @Test
    void cancel_shouldUpdateStatusAndReturn() {
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), eq("reason"), eq(appointmentId))).willReturn(Map.of("id", appointmentId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(appointmentId))).willReturn(Map.of("id", appointmentId, "status_code", "CANCELLED", "order_code", "SO-123", "owner_name", "John"));

        AppointmentReceptionResponse res = service.cancel(appointmentId, new AppointmentCancelRequest("reason"));

        assertThat(res.statusCode()).isEqualTo("CANCELLED");
        verify(jdbc).queryForMap(ArgumentMatchers.contains("UPDATE appointments"), eq("reason"), eq(appointmentId));
    }

    @Test
    void listAppointments_shouldReturnList_WhenKeywordIsNullAndStatusIsEmpty() {
        Map<String, Object> row = Map.of(
                "id", appointmentId,
                "status_code", "PENDING"
        );
        given(jdbc.queryForList(anyString(), any(Object[].class))).willReturn(List.of(row));

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

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM users WHERE phone = ?"), eq("0123456789")))
                .willReturn(Map.of("id", ownerId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM pets WHERE owner_id = ?"), eq(ownerId), eq("Rex")))
                .willReturn(Map.of("id", petId));

        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO service_orders"), any(), eq(ownerId), eq(petId), any(), any(), any(), eq("MED-GENERAL"))).willReturn(Map.of("id", orderId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO appointments"), eq(orderId), any(), any(), any(), eq("Fever"), eq("Note"), any())).willReturn(Map.of("id", newApptId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code FROM appointments WHERE id = ?"), eq(newApptId))).willReturn(Map.of("status_code", "PENDING"));
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), any(), eq(newApptId))).willReturn(Map.of("id", newApptId));

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(newApptId))).willReturn(Map.of("id", newApptId, "status_code", "CHECKED_IN", "order_code", "SO-123", "owner_name", "John"));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

    @Test
    void receive_shouldWork_WhenRequestIsNull() {
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "CONFIRMED"));
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), eq(null), eq(appointmentId))).willReturn(Map.of("id", appointmentId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(appointmentId))).willReturn(Map.of("id", appointmentId, "status_code", "CHECKED_IN", "order_code", "SO-123", "owner_name", "John"));

        AppointmentReceptionResponse res = service.receive(appointmentId, null);
        assertThat(res.statusCode()).isEqualTo("CHECKED_IN");
    }

    @Test
    void cancel_shouldWork_WhenRequestIsNull() {
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), eq(null), eq(appointmentId))).willReturn(Map.of("id", appointmentId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(appointmentId))).willReturn(Map.of("id", appointmentId, "status_code", "CANCELLED", "order_code", "SO-123", "owner_name", "John"));

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

        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM users WHERE phone = ?"), eq("0123456789"))).willReturn(Map.of("id", ownerId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT id FROM pets WHERE owner_id = ?"), eq(ownerId), eq("Rex"))).willReturn(Map.of("id", petId));
        given(securityContextService.getCurrentUserId()).willReturn(UUID.randomUUID());
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO service_orders"), any(), any(), any(), any(), any(), any(), any())).willReturn(Map.of("id", orderId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("INSERT INTO appointments"), any(), any(), any(), any(), any(), any(), any())).willReturn(Map.of("id", newApptId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT status_code FROM appointments WHERE id = ?"), eq(newApptId))).willReturn(Map.of("status_code", "PENDING"));
        given(jdbc.queryForMap(ArgumentMatchers.contains("UPDATE appointments"), any(), eq(newApptId))).willReturn(Map.of("id", newApptId));
        given(jdbc.queryForMap(ArgumentMatchers.contains("SELECT a.id, a.status_code"), eq(newApptId))).willReturn(Map.of("id", newApptId, "status_code", "CHECKED_IN"));

        AppointmentReceptionResponse res = service.quickCreateAndReceive(req);
        assertThat(res.id()).isEqualTo(newApptId);
    }

}