package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GroomingBoardQueryRepository {
    private static final RowMapper<GroomingTicketResponse> TICKET_ROW_MAPPER = (rs, rowNum) ->
            new GroomingTicketResponse(
                    rs.getObject("id", UUID.class),
                    rs.getString("status_code"),
                    rs.getObject("started_at"),
                    rs.getObject("completed_at"),
                    rs.getString("owner_note"),
                    rs.getString("internal_note"),
                    rs.getObject("appointment_id", UUID.class),
                    rs.getObject("scheduled_at"),
                    rs.getString("order_code"),
                    rs.getString("pet_name"),
                    rs.getString("owner_name"),
                    rs.getString("phone"),
                    rs.getString("service_name"),
                    rs.getString("service_code"));
    private static final RowMapper<CompletionNotificationRow> COMPLETION_ROW_MAPPER = (rs, rowNum) ->
            new CompletionNotificationRow(
                    rs.getObject("owner_id", UUID.class),
                    rs.getString("pet_name"));

    private final JdbcTemplate jdbc;

    public List<GroomingTicketResponse> listTickets(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        List<Object> args = new ArrayList<>();
        args.add(q);
        args.add(like(q));
        args.add(like(q));
        args.add("%" + q + "%");
        String statusClause = "";
        String statusFilter = emptyToNull(status);
        if (statusFilter != null) {
            statusClause = "  AND gt.status_code::text = ?\n";
            args.add(statusFilter);
        }
        return jdbc.query("""
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
                """ + statusClause + """
                ORDER BY a.scheduled_start_at, gt.created_at
                """, TICKET_ROW_MAPPER, args.toArray());
    }

    public Optional<GroomingTicketResponse> findTicket(UUID ticketId) {
        return jdbc.query("""
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
                """, TICKET_ROW_MAPPER, ticketId).stream().findFirst();
    }

    public Optional<CompletionNotificationRow> findCompletionNotification(UUID ticketId) {
        return jdbc.query("""
                SELECT so.owner_id, p.name AS pet_name
                FROM grooming_tickets gt
                JOIN appointments a ON a.id = gt.appointment_id
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN pets p ON p.id = so.pet_id
                WHERE gt.id = ?
                """, COMPLETION_ROW_MAPPER, ticketId).stream().findFirst();
    }

    public record CompletionNotificationRow(UUID ownerId, String petName) {
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim().toLowerCase()) + "%";
    }
}
