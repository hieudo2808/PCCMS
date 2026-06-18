package com.astral.express.pccms.reception.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BoardingCareLogCommandRepository {
    private final JdbcTemplate jdbc;

    public Optional<UUID> findLatestActiveSessionIdByPet(UUID petId) {
        return optionalUuid("""
                SELECT bs.id
                FROM boarding_sessions bs
                JOIN boarding_bookings b ON b.id = bs.booking_id
                WHERE b.pet_id = ? AND bs.status_code IN ('CHECKED_IN','IN_STAY','RESERVED')
                ORDER BY bs.created_at DESC
                LIMIT 1
                """, petId);
    }

    public UUID findPetIdBySession(UUID sessionId) {
        return jdbc.queryForObject("""
                SELECT b.pet_id
                FROM boarding_sessions bs
                JOIN boarding_bookings b ON b.id = bs.booking_id
                WHERE bs.id = ?
                """, UUID.class, sessionId);
    }

    public Optional<UUID> findExistingCareLogId(UUID sessionId, LocalDate logDate, String periodCode) {
        return optionalUuid("""
                SELECT id FROM care_logs
                WHERE session_id = ? AND log_date = ? AND period_code = ?::care_period_enum AND deleted_at IS NULL
                """, sessionId, logDate, periodCode);
    }

    public UUID createCareLog(
            UUID sessionId,
            UUID petId,
            UUID staffId,
            UUID workScheduleId,
            LocalDate logDate,
            String periodCode,
            String feedingStatus,
            String hygieneStatus,
            String healthNote,
            String staffNote) {
        return jdbc.queryForObject("""
                INSERT INTO care_logs(session_id, pet_id, staff_id, work_schedule_id, log_date, period_code, feeding_status, hygiene_status, health_note, staff_note)
                VALUES (?, ?, ?, ?, ?, ?::care_period_enum, ?, ?, ?, ?)
                RETURNING id
                """, UUID.class, sessionId, petId, staffId, workScheduleId, logDate, periodCode, feedingStatus,
                hygieneStatus, healthNote, staffNote);
    }

    public void updateCareLog(
            UUID id,
            LocalDate logDate,
            String periodCode,
            String feedingStatus,
            String hygieneStatus,
            String healthNote,
            String staffNote) {
        jdbc.update("""
                UPDATE care_logs
                SET log_date = ?, period_code = ?::care_period_enum, feeding_status = ?, hygiene_status = ?,
                    health_note = ?, staff_note = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, logDate, periodCode, feedingStatus, hygieneStatus, healthNote, staffNote, id);
    }

    public void softDeleteCareLog(UUID id, UUID deletedBy) {
        jdbc.update("""
                UPDATE care_logs
                SET deleted_at = now(), deleted_by = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, deletedBy, id);
    }

    public void insertCareLogMedia(UUID careLogId, UUID fileId, String caption) {
        jdbc.update("INSERT INTO care_log_media(care_log_id, file_id, caption) VALUES (?, ?, ?)",
                careLogId, fileId, caption);
    }

    public Optional<UUID> findActiveWorkScheduleId(UUID staffId, LocalDate logDate) {
        return optionalUuid("""
                SELECT ws.id
                FROM work_schedules ws
                JOIN shifts sh ON sh.id = ws.shift_id
                WHERE ws.staff_id = ? AND ws.work_date = ? AND ws.status_code = 'ASSIGNED'
                  AND CURRENT_TIME BETWEEN sh.start_time AND sh.end_time
                ORDER BY sh.start_time
                LIMIT 1
                """, staffId, logDate);
    }

    public Optional<Boolean> canEditCareLog(UUID careLogId, UUID currentUserId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT CASE
                             WHEN cl.work_schedule_id IS NULL THEN false
                             WHEN cl.staff_id <> ? THEN false
                             WHEN CURRENT_TIMESTAMP > (ws.work_date + sh.end_time)::timestamptz THEN false
                             ELSE true
                           END AS can_edit
                    FROM care_logs cl
                    LEFT JOIN work_schedules ws ON ws.id = cl.work_schedule_id
                    LEFT JOIN shifts sh ON sh.id = ws.shift_id
                    WHERE cl.id = ? AND cl.deleted_at IS NULL
                    """, Boolean.class, currentUserId, careLogId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Optional<UUID> optionalUuid(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, UUID.class, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
