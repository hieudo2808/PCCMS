package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class AppointmentReceptionQueryRepository {
    private static final RowMapper<AppointmentReceptionResponse> APPOINTMENT_ROW_MAPPER = (rs, rowNum) ->
            new AppointmentReceptionResponse(
                    rs.getObject("id", UUID.class),
                    rs.getString("status_code"),
                    rs.getObject("scheduled_start_at"),
                    rs.getObject("scheduled_end_at"),
                    rs.getString("symptom_text"),
                    rs.getString("order_code"),
                    rs.getString("owner_name"),
                    rs.getString("phone"),
                    rs.getString("pet_name"),
                    rs.getString("doctor_name"),
                    rs.getString("service_name"));

    private final JdbcTemplate jdbc;

    public List<AppointmentReceptionResponse> listAppointments(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        List<Object> args = new ArrayList<>();
        args.add(q);
        args.add(like(q));
        args.add(like(q));
        args.add("%" + q + "%");
        String statusClause = "";
        String statusFilter = emptyToNull(status);
        if (statusFilter != null) {
            statusClause = "  AND a.status_code::text = ?\n";
            args.add(statusFilter);
        }
        return jdbc.query("""
                SELECT a.id, a.status_code, a.scheduled_start_at, a.scheduled_end_at, a.symptom_text,
                       so.order_code, u.full_name AS owner_name, u.phone, p.name AS pet_name,
                       vet.full_name AS doctor_name, sc.name AS service_name
                FROM appointments a
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN users u ON u.id = so.owner_id
                JOIN pets p ON p.id = so.pet_id
                JOIN service_catalog sc ON sc.id = so.service_id
                LEFT JOIN users vet ON vet.id = a.assigned_staff_id
                WHERE (? = '' OR lower(u.full_name) LIKE ? OR lower(p.name) LIKE ? OR coalesce(u.phone,'') LIKE ?)
                """ + statusClause + """
                ORDER BY a.scheduled_start_at
                """, APPOINTMENT_ROW_MAPPER, args.toArray());
    }

    public Optional<AppointmentReceptionResponse> findById(UUID appointmentId) {
        return jdbc.query("""
                SELECT a.id, a.status_code, a.scheduled_start_at, a.scheduled_end_at, a.symptom_text,
                       so.order_code, u.full_name AS owner_name, u.phone, p.name AS pet_name,
                       vet.full_name AS doctor_name, sc.name AS service_name
                FROM appointments a
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN users u ON u.id = so.owner_id
                JOIN pets p ON p.id = so.pet_id
                JOIN service_catalog sc ON sc.id = so.service_id
                LEFT JOIN users vet ON vet.id = a.assigned_staff_id
                WHERE a.id = ?
                """, APPOINTMENT_ROW_MAPPER, appointmentId).stream().findFirst();
    }

    public Optional<AppointmentNotificationTarget> findNotificationTarget(UUID appointmentId) {
        return jdbc.query("""
                SELECT so.owner_id, p.name AS pet_name, a.scheduled_start_at
                FROM appointments a
                JOIN service_orders so ON so.id = a.service_order_id
                JOIN pets p ON p.id = so.pet_id
                WHERE a.id = ?
                """, (rs, rowNum) -> new AppointmentNotificationTarget(
                rs.getObject("owner_id", UUID.class),
                rs.getString("pet_name"),
                rs.getObject("scheduled_start_at", OffsetDateTime.class)), appointmentId).stream().findFirst();
    }

    public record AppointmentNotificationTarget(UUID ownerId, String petName, OffsetDateTime scheduledAt) {
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim().toLowerCase()) + "%";
    }
}
