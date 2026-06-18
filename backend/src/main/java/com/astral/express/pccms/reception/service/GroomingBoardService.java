package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.notification.service.NotificationService;
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
    private final NotificationService notificationService;
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
        if ("COMPLETED".equals(request.statusCode())) {
            groomingBoardQueryRepository.findCompletionNotification(ticketId).ifPresent(row -> notificationService.createNotification(
                    row.ownerId(),
                    "GROOMING_TICKET",
                    ticketId,
                    "GROOMING",
                    "Dich vu lam dep hoan thanh",
                    row.petName() + " da hoan thanh dich vu, moi khach den don."
            ));
        }
        return getTicket(ticketId);
    }

    private GroomingTicketResponse getTicket(UUID ticketId) {
        return groomingBoardQueryRepository.findTicket(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND));
    }
}
