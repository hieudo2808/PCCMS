package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.notification.service.BusinessNotificationService;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.reception.repository.GroomingBoardCommandRepository;
import com.astral.express.pccms.reception.repository.GroomingBoardQueryRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroomingBoardServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private BusinessNotificationService businessNotificationService;

    @Mock
    private GroomingBoardQueryRepository groomingBoardQueryRepository;
    @Mock
    private GroomingBoardCommandRepository groomingBoardCommandRepository;

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
        GroomingTicketResponse row = new GroomingTicketResponse(
                ticketId, "PENDING", null, null, null, null, UUID.randomUUID(),
                Timestamp.valueOf(LocalDateTime.now()), "SO-123", "Rex", "John",
                "123456789", "Grooming", "GROOM");
        given(groomingBoardQueryRepository.listTickets(keyword, status)).willReturn(List.of(row));

        // WHEN
        List<GroomingTicketResponse> responses = service.listTickets(keyword, status);

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(ticketId);
    }

    @Test
    void should_ThrowException_when_TicketNotFound() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "started")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_InvalidStatusTransition() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.of("COMPLETED"));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "back to in service")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION);
    }

    @Test
    void should_UpdateStatusToInService_when_ValidRequest() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.of("PENDING"));

        given(groomingBoardQueryRepository.findTicket(ticketId)).willReturn(Optional.of(new GroomingTicketResponse(
                ticketId, "IN_SERVICE", null, null, null, null, UUID.randomUUID(),
                Timestamp.valueOf(LocalDateTime.now()), "SO-123", "Rex", "John",
                "123456789", "Grooming", "GROOM")));

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "Starting grooming"));

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ticketId);
        verifyNoInteractions(businessNotificationService);
    }

    @Test
    void should_UpdateStatusToCompleted_when_ValidRequestAndSendNotification() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.of("IN_SERVICE"));

        UUID ownerId = UUID.randomUUID();
        given(groomingBoardQueryRepository.findCompletionNotification(ticketId))
                .willReturn(Optional.of(new GroomingBoardQueryRepository.CompletionNotificationRow(ownerId, "Rex")));

        given(groomingBoardQueryRepository.findTicket(ticketId)).willReturn(Optional.of(new GroomingTicketResponse(
                ticketId, "COMPLETED", null, null, null, null, UUID.randomUUID(),
                Timestamp.valueOf(LocalDateTime.now()), "SO-123", "Rex", "John",
                "123456789", "Grooming", "GROOM")));

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("COMPLETED", "Done"));

        // THEN
        assertThat(response).isNotNull();
        verify(businessNotificationService).groomingCompleted(ownerId, ticketId, "Rex");
    }

    @Test
    void should_ReturnTickets_when_ListTicketsWithNulls() {
        given(groomingBoardQueryRepository.listTickets(null, null)).willReturn(List.of());
        given(groomingBoardQueryRepository.listTickets(" ", " ")).willReturn(List.of());
        List<GroomingTicketResponse> responses = service.listTickets(null, null);
        assertThat(responses).isEmpty();
        
        List<GroomingTicketResponse> responses2 = service.listTickets(" ", " ");
        assertThat(responses2).isEmpty();
    }

    @Test
    void should_ThrowException_when_GetTicketNotFound() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.of("PENDING"));
        
        // getTicket throws exception
        given(groomingBoardQueryRepository.findTicket(ticketId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateStatus(ticketId, new GroomingStatusUpdateRequest("IN_SERVICE", "Starting grooming")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND);
    }

    @Test
    void should_NotSendNotification_when_CompletedButOwnerNotFound() {
        // GIVEN
        given(groomingBoardCommandRepository.findTicketStatus(ticketId)).willReturn(Optional.of("IN_SERVICE"));

        given(groomingBoardQueryRepository.findCompletionNotification(ticketId)).willReturn(Optional.empty());

        given(groomingBoardQueryRepository.findTicket(ticketId)).willReturn(Optional.of(new GroomingTicketResponse(
                ticketId, "COMPLETED", null, null, null, null, UUID.randomUUID(),
                Timestamp.valueOf(LocalDateTime.now()), "SO-123", "Rex", "John",
                "123456789", "Grooming", "GROOM")));

        // WHEN
        GroomingTicketResponse response = service.updateStatus(ticketId, new GroomingStatusUpdateRequest("COMPLETED", "Done"));

        // THEN
        assertThat(response).isNotNull();
        verifyNoInteractions(businessNotificationService);
    }

}
