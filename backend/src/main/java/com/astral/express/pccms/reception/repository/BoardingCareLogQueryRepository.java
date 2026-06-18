package com.astral.express.pccms.reception.repository;

import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BoardingCareLogQueryRepository {
    private final JdbcTemplate jdbc;

    public List<BoardingBookingResponse> listBookings(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        List<Object> args = new ArrayList<>();
        args.add(q);
        args.add(like(q));
        args.add(like(q));
        args.add("%" + q + "%");
        String statusClause = "";
        String statusFilter = emptyToNull(status);
        if (statusFilter != null) {
            statusClause = "  AND b.status_code::text = ?\n";
            args.add(statusFilter);
        }
        return jdbc.query("""
                SELECT b.id, b.booking_code, b.expected_checkin_at, b.expected_checkout_at, b.status_code,
                       b.special_care_request, b.estimated_price_vnd, p.name AS pet_name, u.full_name AS owner_name,
                       u.phone, rt.name AS room_type_name, bs.id AS session_id, bs.status_code AS session_status
                FROM boarding_bookings b
                JOIN pets p ON p.id = b.pet_id
                JOIN users u ON u.id = b.owner_id
                JOIN room_types rt ON rt.id = b.requested_room_type_id
                LEFT JOIN boarding_sessions bs ON bs.booking_id = b.id
                WHERE (? = '' OR lower(p.name) LIKE ? OR lower(u.full_name) LIKE ? OR coalesce(u.phone,'') LIKE ?)
                """ + statusClause + """
                ORDER BY b.expected_checkin_at DESC
                """, BoardingCareLogQueryRepository::booking, args.toArray());
    }

    public List<CareLogResponse> listCareLogs(UUID currentUserId, UUID sessionId, UUID petId) {
        List<Object> args = new ArrayList<>();
        args.add(currentUserId);
        args.add(currentUserId);
        args.add(currentUserId);
        String clause = buildCareLogListClause(sessionId, petId, args);
        return jdbc.query(careLogSelect(clause), BoardingCareLogQueryRepository::careLog, args.toArray());
    }

    public Optional<CareLogResponse> findCareLog(UUID currentUserId, UUID id) {
        return optional(careLogSelect("WHERE cl.id = ? AND cl.deleted_at IS NULL"),
                BoardingCareLogQueryRepository::careLog,
                currentUserId,
                currentUserId,
                currentUserId,
                id);
    }

    private String buildCareLogListClause(UUID sessionId, UUID petId, List<Object> args) {
        StringBuilder clause = new StringBuilder("WHERE cl.deleted_at IS NULL\n");
        if (sessionId != null) {
            clause.append("  AND cl.session_id = ?\n");
            args.add(sessionId);
        }
        if (petId != null) {
            clause.append("  AND cl.pet_id = ?\n");
            args.add(petId);
        }
        clause.append("ORDER BY cl.log_date DESC, cl.period_code\n");
        return clause.toString();
    }

    private String careLogSelect(String clause) {
        return """
                SELECT cl.id, cl.session_id, cl.pet_id, p.name AS pet_name, u.full_name AS staff_name, cl.log_date,
                       cl.period_code, cl.feeding_status, cl.hygiene_status, cl.health_note, cl.staff_note, cl.created_at,
                       CASE
                         WHEN cl.work_schedule_id IS NULL THEN false
                         WHEN cl.staff_id <> ? THEN false
                         WHEN CURRENT_TIMESTAMP > (ws.work_date + sh.end_time)::timestamptz THEN false
                         ELSE true
                       END AS can_edit,
                       CASE
                         WHEN cl.work_schedule_id IS NULL THEN false
                         WHEN cl.staff_id <> ? THEN false
                         WHEN CURRENT_TIMESTAMP > (ws.work_date + sh.end_time)::timestamptz THEN false
                         ELSE true
                       END AS can_delete,
                       (ws.work_date + sh.end_time)::timestamptz AS locked_at,
                       CASE
                         WHEN cl.work_schedule_id IS NULL THEN 'Khong xac dinh ca tao nhat ky'
                         WHEN cl.staff_id <> ? THEN 'Chi nhan vien tao nhat ky duoc sua'
                         WHEN CURRENT_TIMESTAMP > (ws.work_date + sh.end_time)::timestamptz THEN 'Da het ca lam viec'
                         ELSE NULL
                       END AS locked_reason
                FROM care_logs cl
                JOIN pets p ON p.id = cl.pet_id
                JOIN users u ON u.id = cl.staff_id
                LEFT JOIN work_schedules ws ON ws.id = cl.work_schedule_id
                LEFT JOIN shifts sh ON sh.id = ws.shift_id
                """ + clause;
    }

    private <T> Optional<T> optional(String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static BoardingBookingResponse booking(ResultSet rs, int rowNum) throws SQLException {
        return new BoardingBookingResponse(
                rs.getObject("id", UUID.class),
                rs.getString("booking_code"),
                rs.getObject("expected_checkin_at"),
                rs.getObject("expected_checkout_at"),
                rs.getString("status_code"),
                rs.getString("special_care_request"),
                rs.getObject("estimated_price_vnd"),
                rs.getString("pet_name"),
                rs.getString("owner_name"),
                rs.getString("phone"),
                rs.getString("room_type_name"),
                rs.getObject("session_id", UUID.class),
                rs.getString("session_status")
        );
    }

    private static CareLogResponse careLog(ResultSet rs, int rowNum) throws SQLException {
        return new CareLogResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("session_id", UUID.class),
                rs.getObject("pet_id", UUID.class),
                rs.getString("pet_name"),
                rs.getString("staff_name"),
                rs.getObject("log_date"),
                rs.getString("period_code"),
                rs.getString("feeding_status"),
                rs.getString("hygiene_status"),
                rs.getString("health_note"),
                rs.getString("staff_note"),
                rs.getObject("created_at"),
                rs.getBoolean("can_edit"),
                rs.getBoolean("can_delete"),
                rs.getObject("locked_at"),
                rs.getString("locked_reason")
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String like(String value) {
        return "%" + (value == null ? "" : value.trim().toLowerCase()) + "%";
    }
}
