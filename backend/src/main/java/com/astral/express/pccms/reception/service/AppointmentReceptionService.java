package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import com.astral.express.pccms.reception.service.ReceptionValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppointmentReceptionService {
    private final JdbcTemplate jdbc;
    private final SecurityContextService securityContextService;
@Transactional(readOnly = true)
    public List<AppointmentReceptionResponse> listAppointments(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        return jdbc.queryForList("""
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
                  AND (? IS NULL OR a.status_code::text = ?)
                ORDER BY a.scheduled_start_at
                """, q, like(q), like(q), "%" + q + "%", emptyToNull(status), emptyToNull(status))
                .stream()
                .map(this::appointment)
                .toList();
    }
@Transactional
    public AppointmentReceptionResponse quickCreateAndReceive(QuickAppointmentRequest request) {
        ReceptionValidation.validateQuickAppointment(request.phone(), request.ownerName(), request.petName(), request.symptomText());
        Map<String, Object> owner = optional("SELECT id FROM users WHERE phone = ?", request.phone())
                .orElseGet(() -> jdbc.queryForMap("""
                        INSERT INTO users(email, phone, password_hash, full_name, role_id, status_code)
                        VALUES (?, ?, 'walkin-account', ?, (SELECT id FROM roles WHERE code = 'OWNER'), 'ACTIVE')
                        RETURNING id
                        """, "walkin" + System.currentTimeMillis() + "@pccms.local", request.phone(), request.ownerName()));

        Map<String, Object> pet = optional("SELECT id FROM pets WHERE owner_id = ? AND lower(name) = lower(?)", owner.get("id"), request.petName())
                .orElseGet(() -> jdbc.queryForMap("""
                        INSERT INTO pets(owner_id, name, species_id, sex, estimated_age_months, weight_kg)
                        VALUES (?, ?, (SELECT id FROM pet_species ORDER BY name LIMIT 1), 'UNKNOWN', 12, 1.0)
                        RETURNING id
                        """, owner.get("id"), request.petName()));

        Timestamp start = ts(request.scheduledStartAt());
        if (start == null) {
            start = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        }
        Timestamp end = ts(request.scheduledEndAt());
        if (end == null) {
            end = Timestamp.from(start.toInstant().plusSeconds(1800));
        }

        Map<String, Object> order = jdbc.queryForMap("""
                INSERT INTO service_orders(order_code, owner_id, pet_id, service_id, category_code, status_code, planned_start_at, planned_end_at, base_amount_vnd, created_by)
                SELECT ?, ?, ?, id, 'MEDICAL', 'REQUESTED', ?, ?, base_price_vnd, ? FROM service_catalog WHERE service_code = ?
                RETURNING id
                """, code("SO-AP-"), owner.get("id"), pet.get("id"), start, end, securityContextService.getCurrentUserId(), valueOrDefault(request.serviceCode(), "MED-GENERAL"));

        Map<String, Object> created = jdbc.queryForMap("""
                INSERT INTO appointments(service_order_id, appointment_type, scheduled_start_at, scheduled_end_at, assigned_staff_id, status_code, symptom_text, owner_note, created_by)
                VALUES (?, 'MEDICAL', ?, ?, ?, 'PENDING', ?, ?, ?)
                RETURNING id
                """, order.get("id"), start, end, request.doctorId(), request.symptomText(), request.ownerNote(), securityContextService.getCurrentUserId());

        return receive((UUID) created.get("id"), new AppointmentReceiveRequest(request.doctorId(), "Tạo nhanh tại quầy"));
    }
@Transactional
    public AppointmentReceptionResponse receive(UUID appointmentId, AppointmentReceiveRequest request) {
        Map<String, Object> current = optional("SELECT status_code FROM appointments WHERE id = ?", appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND));
        String status = String.valueOf(current.get("status_code"));
        if ("CANCELLED".equals(status) || !List.of("PENDING", "CONFIRMED").contains(status)) {
            throw new BusinessException(ErrorCode.ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE);
        }
        UUID doctorId = request == null ? null : request.doctorId();
        jdbc.queryForMap("""
                UPDATE appointments
                SET status_code = 'CHECKED_IN', assigned_staff_id = COALESCE(?, assigned_staff_id), updated_at = now()
                WHERE id = ?
                RETURNING id
                """, doctorId, appointmentId);
        jdbc.update("""
                INSERT INTO reception_tickets(appointment_id, checked_in_by, assigned_vet_id, queue_number, note)
                VALUES (?, ?, COALESCE(?, (SELECT assigned_staff_id FROM appointments WHERE id = ?)),
                        COALESCE((SELECT max(queue_number) + 1 FROM reception_tickets), 1), ?)
                ON CONFLICT(appointment_id) DO NOTHING
                """, appointmentId, securityContextService.getCurrentUserId(), doctorId, appointmentId, request == null ? null : request.note());
        return getById(appointmentId);
    }
@Transactional
    public AppointmentReceptionResponse cancel(UUID appointmentId, AppointmentCancelRequest request) {
        jdbc.queryForMap("""
                UPDATE appointments
                SET status_code = 'CANCELLED', internal_note = COALESCE(?, internal_note), updated_at = now()
                WHERE id = ?
                RETURNING id
                """, request == null ? null : request.reason(), appointmentId);
        return getById(appointmentId);
    }

    private AppointmentReceptionResponse getById(UUID appointmentId) {
        return appointment(optional("""
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
                """, appointmentId).orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND)));
    }

    private AppointmentReceptionResponse appointment(Map<String, Object> row) {
        return new AppointmentReceptionResponse(
                (UUID) row.get("id"),
                string(row.get("status_code")),
                row.get("scheduled_start_at"),
                row.get("scheduled_end_at"),
                string(row.get("symptom_text")),
                string(row.get("order_code")),
                string(row.get("owner_name")),
                string(row.get("phone")),
                string(row.get("pet_name")),
                string(row.get("doctor_name")),
                string(row.get("service_name"))
        );
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private Timestamp ts(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() == 10) {
            return Timestamp.valueOf(value + " 00:00:00");
        }
        try {
            return Timestamp.from(OffsetDateTime.parse(value).toInstant());
        } catch (Exception ignored) {
            return Timestamp.valueOf(LocalDateTime.parse(value));
        }
    }

    private String code(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}



