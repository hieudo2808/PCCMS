package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import com.astral.express.pccms.notification.service.NotificationService;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.reception.service.GroomingBoardService;
import com.astral.express.pccms.reception.service.ReceptionValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroomingBoardService {
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;
@Transactional(readOnly = true)
    public List<GroomingTicketResponse> listTickets(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        return jdbc.queryForList("""
                SELECT gt.id, gt.status_code, gt.started_at, gt.completed_at, gt.owner_note, gt.internal_note,
                       a.id AS appointment_id, a.scheduled_start_at AS scheduled_at,
                       so.order_code, p.name AS pet_name, u.full_name AS owner_name, u.phone,
                       sc.name AS service_name, sc.service_code
                FROM grooming_tickets gt
                JOIN appointments a ON a.id = gt.appointment_id
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN pets p ON p.id = so.pet_id
                JOIN users u ON u.id = so.owner_id
                JOIN service_catalog sc ON sc.id = so.service_id
                WHERE (? = '' OR lower(p.name) LIKE ? OR lower(u.full_name) LIKE ? OR coalesce(u.phone,'') LIKE ?)
                  AND (? IS NULL OR gt.status_code::text = ?)
                ORDER BY a.scheduled_start_at, gt.created_at
                """, q, like(q), like(q), "%" + q + "%", emptyToNull(status), emptyToNull(status))
                .stream()
                .map(this::ticket)
                .toList();
    }
@Transactional
    public GroomingTicketResponse updateStatus(UUID ticketId, GroomingStatusUpdateRequest request) {
        Map<String, Object> current = optional("SELECT status_code FROM grooming_tickets WHERE id = ?", ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND));
        String currentStatus = string(current.get("status_code"));
        ReceptionValidation.validateGroomingTransition(currentStatus, request.statusCode());
        jdbc.queryForMap("""
                UPDATE grooming_tickets
                SET status_code = ?::grooming_status_enum,
                    internal_note = COALESCE(?, internal_note),
                    started_at = CASE WHEN ? = 'IN_SERVICE' AND started_at IS NULL THEN now() ELSE started_at END,
                    completed_at = CASE WHEN ? = 'COMPLETED' THEN now() ELSE completed_at END,
                    updated_at = now()
                WHERE id = ?
                RETURNING id
                """, request.statusCode(), request.internalNote(), request.statusCode(), request.statusCode(), ticketId);
        if ("COMPLETED".equals(request.statusCode())) {
            optional("""
                    SELECT so.owner_id, p.name AS pet_name
                    FROM grooming_tickets gt
                    JOIN appointments a ON a.id = gt.appointment_id
                    JOIN service_orders so ON so.id = a.service_order_id
                    JOIN pets p ON p.id = so.pet_id
                    WHERE gt.id = ?
                    """, ticketId).ifPresent(row -> notificationService.createNotification(
                    (UUID) row.get("owner_id"),
                    "GROOMING_TICKET",
                    ticketId,
                    "GROOMING",
                    "Dich vu lam dep hoan thanh",
                    string(row.get("pet_name")) + " da hoan thanh dich vu, moi khach den don."
            ));
        }
        return getTicket(ticketId);
    }

    private GroomingTicketResponse getTicket(UUID ticketId) {
        return ticket(optional("""
                SELECT gt.id, gt.status_code, gt.started_at, gt.completed_at, gt.owner_note, gt.internal_note,
                       a.id AS appointment_id, a.scheduled_start_at AS scheduled_at,
                       so.order_code, p.name AS pet_name, u.full_name AS owner_name, u.phone,
                       sc.name AS service_name, sc.service_code
                FROM grooming_tickets gt
                JOIN appointments a ON a.id = gt.appointment_id
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN pets p ON p.id = so.pet_id
                JOIN users u ON u.id = so.owner_id
                JOIN service_catalog sc ON sc.id = so.service_id
                WHERE gt.id = ?
                """, ticketId).orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND)));
    }

    private GroomingTicketResponse ticket(Map<String, Object> row) {
        return new GroomingTicketResponse(
                (UUID) row.get("id"), string(row.get("status_code")), row.get("started_at"), row.get("completed_at"),
                string(row.get("owner_note")), string(row.get("internal_note")), (UUID) row.get("appointment_id"),
                row.get("scheduled_at"), string(row.get("order_code")), string(row.get("pet_name")), string(row.get("owner_name")),
                string(row.get("phone")), string(row.get("service_name")), string(row.get("service_code"))
        );
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Optional<Map<String, Object>> optional(String sql, Object... args) {
        try {
            return Optional.of(jdbc.queryForMap(sql, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim().toLowerCase()) + "%";
    }
}


