package com.astral.express.pccms.reception.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AppointmentReceptionCommandRepository {
    private final JdbcTemplate jdbc;

    public Optional<UUID> findOwnerIdByPhone(String phone) {
        return optionalUuid("SELECT id FROM users WHERE phone = ?", phone);
    }

    public UUID createWalkinOwner(String email, String phone, String ownerName) {
        return jdbc.queryForObject("""
                INSERT INTO users(email, phone, password_hash, full_name, role_id, status_code)
                VALUES (?, ?, 'walkin-account', ?, (SELECT id FROM roles WHERE code = 'OWNER'), 'ACTIVE')
                RETURNING id
                """, UUID.class, email, phone, ownerName);
    }

    public Optional<UUID> findPetId(UUID ownerId, String petName) {
        return optionalUuid("SELECT id FROM pets WHERE owner_id = ? AND lower(name) = lower(?)", ownerId, petName);
    }

    public UUID createPet(UUID ownerId, String petName) {
        return jdbc.queryForObject("""
                INSERT INTO pets(owner_id, name, species_id, sex, estimated_age_months, weight_kg)
                VALUES (?, ?, (SELECT id FROM pet_species ORDER BY name LIMIT 1), 'UNKNOWN', 12, 1.0)
                RETURNING id
                """, UUID.class, ownerId, petName);
    }

    public UUID createServiceOrder(
            String orderCode,
            UUID ownerId,
            UUID petId,
            Timestamp start,
            Timestamp end,
            UUID createdBy,
            String serviceCode) {
        return jdbc.queryForObject("""
                INSERT INTO service_orders(order_code, owner_id, pet_id, service_id, category_code, status_code, planned_start_at, planned_end_at, base_amount_vnd, created_by)
                SELECT ?, ?, ?, id, 'MEDICAL', 'REQUESTED', ?, ?, base_price_vnd, ? FROM service_catalog WHERE service_code = ?
                RETURNING id
                """, UUID.class, orderCode, ownerId, petId, start, end, createdBy, serviceCode);
    }

    public UUID createAppointment(
            UUID serviceOrderId,
            Timestamp start,
            Timestamp end,
            UUID doctorId,
            String symptomText,
            String ownerNote,
            UUID createdBy) {
        return jdbc.queryForObject("""
                INSERT INTO appointments(service_order_id, appointment_type, scheduled_start_at, scheduled_end_at, assigned_staff_id, status_code, symptom_text, owner_note, created_by)
                VALUES (?, 'MEDICAL', ?, ?, ?, 'PENDING', ?, ?, ?)
                RETURNING id
                """, UUID.class, serviceOrderId, start, end, doctorId, symptomText, ownerNote, createdBy);
    }

    public Optional<String> findAppointmentStatus(UUID appointmentId) {
        return optionalString("SELECT status_code FROM appointments WHERE id = ?", appointmentId);
    }

    public void receiveAppointment(UUID appointmentId, UUID doctorId) {
        jdbc.queryForObject("""
                UPDATE appointments
                SET status_code = 'CHECKED_IN', assigned_staff_id = COALESCE(?, assigned_staff_id), updated_at = now()
                WHERE id = ?
                RETURNING id
                """, UUID.class, doctorId, appointmentId);
    }

    public void createReceptionTicket(UUID appointmentId, UUID checkedInBy, UUID doctorId, String note) {
        jdbc.update("""
                INSERT INTO reception_tickets(appointment_id, checked_in_by, assigned_vet_id, queue_number, note)
                VALUES (?, ?, COALESCE(?, (SELECT assigned_staff_id FROM appointments WHERE id = ?)),
                        COALESCE((SELECT max(queue_number) + 1 FROM reception_tickets), 1), ?)
                ON CONFLICT(appointment_id) DO NOTHING
                """, appointmentId, checkedInBy, doctorId, appointmentId, note);
    }

    public void cancelAppointment(UUID appointmentId, String reason) {
        jdbc.queryForObject("""
                UPDATE appointments
                SET status_code = 'CANCELLED', internal_note = COALESCE(?, internal_note), updated_at = now()
                WHERE id = ?
                RETURNING id
                """, UUID.class, reason, appointmentId);
    }

    private Optional<UUID> optionalUuid(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, UUID.class, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Optional<String> optionalString(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, String.class, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
