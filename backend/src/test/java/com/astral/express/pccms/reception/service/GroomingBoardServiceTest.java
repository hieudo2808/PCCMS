package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.notification.service.NotificationService;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verifyNoInteractions;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroomingBoardServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GroomingBoardService service;

    private UUID ticketId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
    }

    @Test
    void should_ReturnTickets_when_ListTickets() {
        // GIVEN
        String keyword = "John";
        String status = "PENDING";
        Map<String, Object> row = Map.of(
                "id", ticketId,
                "status_code", "PENDING",
                "appointment_id", UUID.randomUUID(),
                "scheduled_at", Timestamp.valueOf(LocalDateTime.now()),
                "order_code", "SO-123",
                "pet_name", "Rex",
                "owner_name", "John",
                "phone", "123456789",
                "service_name", "Grooming",
                "service_code", "GROOM"
        );
        given(jdbc.queryForList(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).willReturn(List.of(row));

        // WHEN
        List<GroomingTicketResponse> responses = service.listTickets(keyword, status);

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(ticketId);
    }

    @Test
    void should_ThrowException_when_TicketNotFound() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "started")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_InvalidStatusTransition() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "COMPLETED"));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "back to in service")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION);
    }

    @Test
    void should_UpdateStatusToInService_when_ValidRequest() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "PENDING"));
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("UPDATE grooming_tickets"), any(), any(), any(), any(), any())).willReturn(Map.of("id", ticketId));

        Map<String, Object> row = Map.of(
                "id", ticketId,
                "status_code", "IN_SERVICE",
                "appointment_id", UUID.randomUUID(),
                "scheduled_at", Timestamp.valueOf(LocalDateTime.now()),
                "order_code", "SO-123",
                "pet_name", "Rex",
                "owner_name", "John",
                "phone", "123456789",
                "service_name", "Grooming",
                "service_code", "GROOM"
        );
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT gt.id"), any())).willReturn(row);

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "Starting grooming"));

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ticketId);
        verifyNoInteractions(notificationService);
    }

    @Test
    void should_UpdateStatusToCompleted_when_ValidRequestAndSendNotification() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "IN_SERVICE"));
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("UPDATE grooming_tickets"), any(), any(), any(), any(), any())).willReturn(Map.of("id", ticketId));

        UUID ownerId = UUID.randomUUID();
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT so.owner_id"), any())).willReturn(Map.of(
                "owner_id", ownerId,
                "pet_name", "Rex"
        ));

        Map<String, Object> row = Map.of(
                "id", ticketId,
                "status_code", "COMPLETED",
                "appointment_id", UUID.randomUUID(),
                "scheduled_at", Timestamp.valueOf(LocalDateTime.now()),
                "order_code", "SO-123",
                "pet_name", "Rex",
                "owner_name", "John",
                "phone", "123456789",
                "service_name", "Grooming",
                "service_code", "GROOM"
        );
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT gt.id"), any())).willReturn(row);

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("COMPLETED", "Done"));

        // THEN
        assertThat(response).isNotNull();
        verify(notificationService).createNotification(
                eq(ownerId),
                eq("GROOMING_TICKET"),
                eq(ticketId),
                eq("GROOMING"),
                anyString(),
                org.mockito.ArgumentMatchers.contains("Rex")
        );
    }

    @Test
    void should_ReturnTickets_when_ListTicketsWithNulls() {
        given(jdbc.queryForList(anyString(), org.mockito.ArgumentMatchers.any(Object[].class))).willReturn(List.of());
        List<GroomingTicketResponse> responses = service.listTickets(null, null);
        assertThat(responses).isEmpty();
        
        List<GroomingTicketResponse> responses2 = service.listTickets(" ", " ");
        assertThat(responses2).isEmpty();
    }

    @Test
    void should_ThrowException_when_GetTicketNotFound() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "PENDING"));
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("UPDATE grooming_tickets"), any(), any(), any(), any(), any())).willReturn(Map.of("id", ticketId));
        
        // getTicket throws exception
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT gt.id"), any())).willThrow(new EmptyResultDataAccessException(1));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "Starting grooming")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND);
    }

    @Test
    void should_NotSendNotification_when_CompletedButOwnerNotFound() {
        // GIVEN
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT status_code"), any())).willReturn(Map.of("status_code", "IN_SERVICE"));
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("UPDATE grooming_tickets"), any(), any(), any(), any(), any())).willReturn(Map.of("id", ticketId));

        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT so.owner_id"), any())).willThrow(new EmptyResultDataAccessException(1));

        Map<String, Object> row = Map.of(
                "id", ticketId,
                "status_code", "COMPLETED",
                "appointment_id", UUID.randomUUID(),
                "scheduled_at", Timestamp.valueOf(LocalDateTime.now()),
                "order_code", "SO-123",
                "pet_name", "Rex",
                "owner_name", "John",
                "phone", "123456789",
                "service_name", "Grooming",
                "service_code", "GROOM"
        );
        given(jdbc.queryForMap(org.mockito.ArgumentMatchers.contains("SELECT gt.id"), any())).willReturn(row);

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("COMPLETED", "Done"));

        // THEN
        assertThat(response).isNotNull();
        verifyNoInteractions(notificationService);
    }

}