package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.request.CareLogRequest;
import com.astral.express.pccms.reception.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.reception.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.reception.dto.response.CareLogResponse;
import com.astral.express.pccms.reception.service.BoardingCareLogService;
import com.astral.express.pccms.reception.service.ReceptionValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardingCareLogService {
    private final JdbcTemplate jdbc;
    private final SecurityContextService securityContextService;
    private final FileMediaService fileMediaService;
@Transactional(readOnly = true)
    public List<BoardingBookingResponse> listBookings(String keyword, String status) {
        String q = keyword == null ? "" : keyword.trim();
        return jdbc.queryForList("""
                SELECT b.id, b.booking_code, b.expected_checkin_at, b.expected_checkout_at, b.status_code,
                       b.special_care_request, b.estimated_price_vnd, p.name AS pet_name, u.full_name AS owner_name,
                       u.phone, rt.name AS room_type_name, bs.id AS session_id, bs.status_code AS session_status
                FROM boarding_bookings b
                JOIN pets p ON p.id = b.pet_id
                JOIN users u ON u.id = b.owner_id
                JOIN room_types rt ON rt.id = b.requested_room_type_id
                LEFT JOIN boarding_sessions bs ON bs.booking_id = b.id
                WHERE (? = '' OR lower(p.name) LIKE ? OR lower(u.full_name) LIKE ? OR coalesce(u.phone,'') LIKE ?)
                  AND (? IS NULL OR b.status_code::text = ?)
                ORDER BY b.expected_checkin_at DESC
                """, q, like(q), like(q), "%" + q + "%", emptyToNull(status), emptyToNull(status))
                .stream()
                .map(this::booking)
                .toList();
    }
@Transactional(readOnly = true)
    public List<CareLogResponse> listCareLogs(UUID sessionId, UUID petId) {
        return jdbc.queryForList("""
                SELECT cl.id, cl.session_id, cl.pet_id, p.name AS pet_name, u.full_name AS staff_name, cl.log_date,
                       cl.period_code, cl.feeding_status, cl.hygiene_status, cl.health_note, cl.staff_note, cl.created_at
                FROM care_logs cl
                JOIN pets p ON p.id = cl.pet_id
                JOIN users u ON u.id = cl.staff_id
                WHERE (? IS NULL OR cl.session_id = ?) AND (? IS NULL OR cl.pet_id = ?)
                ORDER BY cl.log_date DESC, cl.period_code
                """, sessionId, sessionId, petId, petId)
                .stream()
                .map(this::careLog)
                .toList();
    }
@Transactional
    public CareLogResponse saveCareLog(CareLogRequest request) {
        LocalDate logDate = date(request.logDate(), LocalDate.now());
        ReceptionValidation.validateCareLog(logDate, request.periodCode(), request.feedingStatus(), request.hygieneStatus());
        UUID sessionId = request.sessionId();
        UUID petId = request.petId();
        if (sessionId == null && petId == null) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        if (sessionId == null) {
            Map<String, Object> session = optional("""
                    SELECT bs.id
                    FROM boarding_sessions bs
                    JOIN boarding_bookings b ON b.id = bs.booking_id
                    WHERE b.pet_id = ? AND bs.status_code IN ('CHECKED_IN','IN_STAY','RESERVED')
                    ORDER BY bs.created_at DESC
                    LIMIT 1
                    """, petId).orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_004_ACTIVE_BOARDING_SESSION_NOT_FOUND));
            sessionId = (UUID) session.get("id");
        }
        if (petId == null) {
            Map<String, Object> sessionPet = jdbc.queryForMap("""
                    SELECT b.pet_id
                    FROM boarding_sessions bs
                    JOIN boarding_bookings b ON b.id = bs.booking_id
                    WHERE bs.id = ?
                    """, sessionId);
            petId = (UUID) sessionPet.get("pet_id");
        }
        Map<String, Object> row = jdbc.queryForMap("""
                INSERT INTO care_logs(session_id, pet_id, staff_id, log_date, period_code, feeding_status, hygiene_status, health_note, staff_note)
                VALUES (?, ?, ?, ?, ?::care_period_enum, ?, ?, ?, ?)
                ON CONFLICT(session_id, log_date, period_code) DO UPDATE SET
                    staff_id = excluded.staff_id,
                    feeding_status = excluded.feeding_status,
                    hygiene_status = excluded.hygiene_status,
                    health_note = excluded.health_note,
                    staff_note = excluded.staff_note,
                    updated_at = now()
                RETURNING id
                """, sessionId, petId, securityContextService.getCurrentUserId(), logDate, request.periodCode(), request.feedingStatus(),
                request.hygieneStatus(), request.healthNote(), request.staffNote());
        return getCareLog((UUID) row.get("id"));
    }
@Transactional
    public CareLogMediaResponse uploadMedia(UUID careLogId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        ReceptionValidation.validateCareLogMedia(file.getSize(), file.getContentType());
        UUID currentUserId = securityContextService.getCurrentUserId();
        UploadedFileResponse uploaded = fileMediaService.uploadOwnerVisibleMedia(file, currentUserId);
        jdbc.update("INSERT INTO care_log_media(care_log_id, file_id, caption) VALUES (?, ?, ?)",
                careLogId, uploaded.id(), "Anh/video nhat ky luu tru");
        return new CareLogMediaResponse(
                uploaded.id(),
                file.getOriginalFilename() == null ? "care-log-image" : file.getOriginalFilename(),
                uploaded.url(),
                uploaded.mimeType(),
                uploaded.sizeBytes(),
                currentUserId,
                "OWNER_VISIBLE"
        );
    }

    private CareLogResponse getCareLog(UUID id) {
        return careLog(optional("""
                SELECT cl.id, cl.session_id, cl.pet_id, p.name AS pet_name, u.full_name AS staff_name, cl.log_date,
                       cl.period_code, cl.feeding_status, cl.hygiene_status, cl.health_note, cl.staff_note, cl.created_at
                FROM care_logs cl
                JOIN pets p ON p.id = cl.pet_id
                JOIN users u ON u.id = cl.staff_id
                WHERE cl.id = ?
                """, id).orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG)));
    }

    private BoardingBookingResponse booking(Map<String, Object> row) {
        return new BoardingBookingResponse(
                (UUID) row.get("id"), string(row.get("booking_code")), row.get("expected_checkin_at"), row.get("expected_checkout_at"),
                string(row.get("status_code")), string(row.get("special_care_request")), row.get("estimated_price_vnd"),
                string(row.get("pet_name")), string(row.get("owner_name")), string(row.get("phone")), string(row.get("room_type_name")),
                (UUID) row.get("session_id"), string(row.get("session_status"))
        );
    }

    private CareLogResponse careLog(Map<String, Object> row) {
        return new CareLogResponse(
                (UUID) row.get("id"), (UUID) row.get("session_id"), (UUID) row.get("pet_id"), string(row.get("pet_name")),
                string(row.get("staff_name")), row.get("log_date"), string(row.get("period_code")), string(row.get("feeding_status")),
                string(row.get("hygiene_status")), string(row.get("health_note")), string(row.get("staff_note")), row.get("created_at")
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

    private LocalDate date(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(value);
    }
}


