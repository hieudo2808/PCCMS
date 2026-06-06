package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.UpsertCareLogRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.boarding.support.BoardingPeriodLabels;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardingStaffServiceImpl implements BoardingStaffService {

    private static final List<String> ACTIVE_SESSION_STATUSES = List.of("CHECKED_IN", "IN_STAY");
    private static final List<String> ALL_PERIODS = List.of("MORNING", "NOON", "AFTERNOON");

    private final CareLogRepository careLogRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffBoardingStayResponse> listActiveStays() {
        return careLogRepository.findActiveStaysForStaff().stream()
                .map(this::mapStaffStayRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareLogResponse> listSessionLogs(UUID sessionId, LocalDate logDate) {
        LocalDate resolvedDate = logDate != null ? logDate : LocalDate.now();
        assertActiveSession(sessionId);

        List<CareLog> logs = careLogRepository.findBySessionIdAndLogDateOrderByPeriodCodeDesc(sessionId, resolvedDate);
        if (logs.isEmpty()) {
            return List.of();
        }

        UUID petId = logs.get(0).getPetId();
        String petName = petRepository.findById(petId).map(Pets::getName).orElse("");

        return logs.stream()
                .map(log -> toCareLogResponse(log, petName))
                .toList();
    }

    @Override
    @Transactional
    public CareLogResponse upsertCareLog(UUID staffId, UpsertCareLogRequest request) {
        SessionContext session = assertActiveSession(request.sessionId());
        LocalDate logDate = request.logDate() != null ? request.logDate() : LocalDate.now();

        CareLog careLog = careLogRepository
                .findBySessionIdAndLogDateAndPeriodCode(session.sessionId(), logDate, request.periodCode())
                .orElseGet(CareLog::new);

        careLog.setSessionId(session.sessionId());
        careLog.setPetId(session.petId());
        careLog.setStaffId(staffId);
        careLog.setLogDate(logDate);
        careLog.setPeriodCode(request.periodCode());
        careLog.setFeedingStatus(request.feedingStatus().trim());
        careLog.setHygieneStatus(request.hygieneStatus().trim());
        careLog.setHealthNote(trimToNull(request.healthNote()));
        careLog.setStaffNote(trimToNull(request.staffNote()));

        CareLog saved = careLogRepository.save(careLog);
        String petName = petRepository.findById(session.petId()).map(Pets::getName).orElse("");
        return toCareLogResponse(saved, petName);
    }

    private StaffBoardingStayResponse mapStaffStayRow(Object[] row) {
        UUID sessionId = (UUID) row[0];
        UUID petId = (UUID) row[1];
        String petName = row[2] != null ? (String) row[2] : "";
        String roomLabel = row[3] != null ? (String) row[3] : "—";

        LocalDate checkinDate = toLocalDate(row[4]);
        if (checkinDate == null) {
            checkinDate = toLocalDate(row[5]);
        }
        LocalDate checkoutDate = toLocalDate(row[6]);

        int totalDays = 1;
        if (checkinDate != null && checkoutDate != null) {
            totalDays = Math.max(1, (int) ChronoUnit.DAYS.between(checkinDate, checkoutDate) + 1);
        }

        int currentDay = 1;
        if (checkinDate != null) {
            currentDay = Math.min(totalDays, Math.max(1, (int) ChronoUnit.DAYS.between(checkinDate, LocalDate.now()) + 1));
        }

        String todayPeriods = row[7] != null ? (String) row[7] : "";
        String todayLogSummary = buildTodayLogSummary(todayPeriods);

        return new StaffBoardingStayResponse(
                sessionId,
                petId,
                petName,
                roomLabel,
                currentDay,
                totalDays,
                todayLogSummary
        );
    }

    private static String buildTodayLogSummary(String todayPeriods) {
        if (todayPeriods == null || todayPeriods.isBlank()) {
            return "Chưa cập nhật nhật ký hôm nay";
        }

        List<String> logged = Arrays.stream(todayPeriods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(BoardingPeriodLabels::toPeriodLabel)
                .toList();

        List<String> missing = ALL_PERIODS.stream()
                .filter(period -> !todayPeriods.contains(period))
                .map(BoardingPeriodLabels::toPeriodLabel)
                .toList();

        if (missing.isEmpty()) {
            return "Đã cập nhật đủ 3 buổi";
        }
        if (logged.size() == 1) {
            return "Đã cập nhật " + logged.get(0).toLowerCase();
        }
        return "Đã cập nhật: " + String.join(", ", logged);
    }

    private SessionContext assertActiveSession(UUID sessionId) {
        Object[] row = careLogRepository.findSessionContext(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BRG_001_SESSION_NOT_FOUND));

        String status = row[2] != null ? row[2].toString() : "";
        if (!ACTIVE_SESSION_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.ERR_BRG_002_SESSION_NOT_ACTIVE);
        }

        return new SessionContext((UUID) row[0], (UUID) row[1]);
    }

    private static CareLogResponse toCareLogResponse(CareLog log, String petName) {
        return new CareLogResponse(
                log.getId(),
                log.getPetId(),
                petName,
                log.getLogDate(),
                log.getPeriodCode(),
                BoardingPeriodLabels.toPeriodLabel(log.getPeriodCode()),
                log.getFeedingStatus(),
                log.getHygieneStatus(),
                log.getHealthNote(),
                log.getStaffNote(),
                Collections.emptyList()
        );
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SessionContext(UUID sessionId, UUID petId) {}
}
