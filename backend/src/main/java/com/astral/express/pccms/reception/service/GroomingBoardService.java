package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.notification.service.BusinessNotificationService;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.reception.repository.GroomingBoardCommandRepository;
import com.astral.express.pccms.reception.repository.GroomingBoardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroomingBoardService {
    private final BusinessNotificationService businessNotificationService;
    private final GroomingBoardQueryRepository groomingBoardQueryRepository;
    private final GroomingBoardCommandRepository groomingBoardCommandRepository;

    @Transactional(readOnly = true)
    public List<GroomingTicketResponse> listTickets(String keyword, String status) {
        return groomingBoardQueryRepository.listTickets(keyword, status);
    }

    @Transactional
    public GroomingTicketResponse updateStatus(UUID ticketId, GroomingStatusUpdateRequest request) {
        String currentStatus = groomingBoardCommandRepository.findTicketStatus(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND));
        ReceptionValidation.validateGroomingTransition(currentStatus, request.statusCode());
        groomingBoardCommandRepository.updateTicketStatus(ticketId, request.statusCode(), request.internalNote());
        if ("COMPLETED".equals(request.statusCode()) || "CANCELLED".equals(request.statusCode())) {
            groomingBoardQueryRepository.findCompletionNotification(ticketId).ifPresent(row -> notificationService(
                    row.ownerId(), ticketId, row.petName(), request.statusCode()));
        }
        return getTicket(ticketId);
    }

    private void notificationService(UUID ownerId, UUID ticketId, String petName, String statusCode) {
        if ("COMPLETED".equals(statusCode)) {
            businessNotificationService.groomingCompleted(ownerId, ticketId, petName);
        } else {
            businessNotificationService.groomingCancelled(ownerId, ticketId, petName);
        }
    }

    private GroomingTicketResponse getTicket(UUID ticketId) {
        return groomingBoardQueryRepository.findTicket(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND));
    }
}
