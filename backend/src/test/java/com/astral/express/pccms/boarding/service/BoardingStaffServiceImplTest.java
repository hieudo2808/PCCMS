package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.UpsertCareLogRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.boarding.support.BoardingPeriodLabels;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardingStaffServiceImplTest {

    @Mock
    private CareLogRepository careLogRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private BoardingSessionRepository boardingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BoardingStaffServiceImpl boardingStaffService;

    @Test
    void should_ListActiveStays_Success() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        CareLogRepository.StaffActiveStayRow row = staffStayRow(
                sessionId,
                petId,
                "Milo",
                "Room 101",
                java.sql.Date.valueOf(LocalDate.now().minusDays(2)),
                null,
                java.sql.Date.valueOf(LocalDate.now().plusDays(2)),
                "MORNING,NOON"
        );
        given(careLogRepository.findActiveStaysForStaff()).willReturn(List.of(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).petName()).isEqualTo("Milo");
        assertThat(responses.get(0).todayLogSummary()).contains(
                BoardingPeriodLabels.toPeriodLabel("MORNING"),
                BoardingPeriodLabels.toPeriodLabel("NOON")
        );
    }

    @Test
    void should_ListSessionLogs_Success() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, petId, "IN_STAY")));

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setName("Milo");

        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Staff A");

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);

        CareLog log = new CareLog();
        log.setId(UUID.randomUUID());
        log.setSession(session);
        log.setPet(pet);
        log.setStaff(staff);
        log.setPeriodCode(CarePeriod.MORNING);
        log.setFeedingStatus("GOOD");
        log.setHygieneStatus("CLEAN");

        given(careLogRepository.findBySessionIdAndLogDateAndDeletedAtIsNullOrderByPeriodCodeDesc(
                eq(sessionId),
                any(LocalDate.class)
        )).willReturn(List.of(log));
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        List<CareLogResponse> responses = boardingStaffService.listSessionLogs(sessionId, LocalDate.now());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).periodCode()).isEqualTo(CarePeriod.MORNING);
    }

    @Test
    void should_ThrowException_when_ListSessionLogs_SessionNotActive() {
        UUID sessionId = UUID.randomUUID();
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, UUID.randomUUID(), "COMPLETED")));

        assertThatThrownBy(() -> boardingStaffService.listSessionLogs(sessionId, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BRG_002_SESSION_NOT_ACTIVE);
    }

    @Test
    void should_UpsertCareLog_Success() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();

        UpsertCareLogRequest request = new UpsertCareLogRequest(
                sessionId,
                LocalDate.now(),
                "MORNING",
                "GOOD",
                "CLEAN",
                "All good",
                null
        );
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, petId, "IN_STAY")));
        given(careLogRepository.findBySessionIdAndLogDateAndPeriodCodeAndDeletedAtIsNull(
                eq(sessionId),
                any(LocalDate.class),
                eq(CarePeriod.MORNING)
        )).willReturn(Optional.empty());

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);
        given(boardingSessionRepository.getReferenceById(sessionId)).willReturn(session);

        Pets pet = new Pets();
        pet.setId(petId);
        given(petRepository.getReferenceById(petId)).willReturn(pet);

        Users staff = new Users();
        staff.setId(staffId);
        staff.setFullName("Staff B");
        given(userRepository.getReferenceById(staffId)).willReturn(staff);

        CareLog savedLog = new CareLog();
        savedLog.setId(UUID.randomUUID());
        savedLog.setSession(session);
        savedLog.setPet(pet);
        savedLog.setStaff(staff);
        savedLog.setLogDate(LocalDate.now());
        savedLog.setPeriodCode(CarePeriod.MORNING);
        savedLog.setFeedingStatus("GOOD");
        savedLog.setHygieneStatus("CLEAN");

        given(careLogRepository.save(any(CareLog.class))).willReturn(savedLog);
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        CareLogResponse response = boardingStaffService.upsertCareLog(staffId, request);

        assertThat(response.feedingStatus()).isEqualTo("GOOD");
        assertThat(response.periodCode()).isEqualTo(CarePeriod.MORNING);
        verify(careLogRepository).save(any(CareLog.class));
    }

    @Test
    void listSessionLogs_shouldUseNow_whenLogDateIsNull() {
        UUID sessionId = UUID.randomUUID();
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, UUID.randomUUID(), "IN_STAY")));
        given(careLogRepository.findBySessionIdAndLogDateAndDeletedAtIsNullOrderByPeriodCodeDesc(
                eq(sessionId),
                any(LocalDate.class)
        )).willReturn(List.of());

        List<CareLogResponse> responses = boardingStaffService.listSessionLogs(sessionId, null);

        assertThat(responses).isEmpty();
    }

    @Test
    void listActiveStays_shouldHandleNullsAndVariousDateTypes() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        CareLogRepository.StaffActiveStayRow row = staffStayRow(
                sessionId,
                petId,
                null,
                null,
                null,
                new java.util.Date(),
                new Timestamp(System.currentTimeMillis()),
                null
        );
        given(careLogRepository.findActiveStaysForStaff()).willReturn(List.of(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).petName()).isEqualTo("");
        assertThat(responses.get(0).roomLabel()).isEqualTo("—");
        assertThat(responses.get(0).todayLogSummary()).isEqualTo("Chưa cập nhật nhật ký hôm nay");
    }

    @Test
    void listActiveStays_shouldHandleOnePeriodLogged() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        CareLogRepository.StaffActiveStayRow row = staffStayRow(
                sessionId,
                petId,
                "Milo",
                "Room 101",
                java.sql.Date.valueOf(LocalDate.now()),
                null,
                null,
                "MORNING"
        );
        given(careLogRepository.findActiveStaysForStaff()).willReturn(List.of(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses.get(0).todayLogSummary())
                .endsWith(BoardingPeriodLabels.toPeriodLabel("MORNING").toLowerCase());
    }

    @Test
    void listActiveStays_shouldHandleAllPeriodsLogged() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        CareLogRepository.StaffActiveStayRow row = staffStayRow(
                sessionId,
                petId,
                "Milo",
                "Room 101",
                java.sql.Date.valueOf(LocalDate.now()),
                null,
                null,
                "MORNING,NOON,AFTERNOON"
        );
        given(careLogRepository.findActiveStaysForStaff()).willReturn(List.of(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses.get(0).todayLogSummary()).isEqualTo("Đã cập nhật đủ 3 buổi");
    }

    @Test
    void assertActiveSession_shouldHandleNullStatus() {
        UUID sessionId = UUID.randomUUID();
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, UUID.randomUUID(), null)));

        assertThatThrownBy(() -> boardingStaffService.listSessionLogs(sessionId, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BRG_002_SESSION_NOT_ACTIVE);
    }

    @Test
    void upsertCareLog_shouldTrimEmptyHealthNoteToNull() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();

        UpsertCareLogRequest request = new UpsertCareLogRequest(
                sessionId,
                null,
                "MORNING",
                "GOOD",
                "CLEAN",
                "   ",
                "   "
        );
        given(careLogRepository.findSessionContext(sessionId))
                .willReturn(Optional.of(sessionContext(sessionId, petId, "IN_STAY")));
        given(careLogRepository.findBySessionIdAndLogDateAndPeriodCodeAndDeletedAtIsNull(
                eq(sessionId),
                any(LocalDate.class),
                eq(CarePeriod.MORNING)
        )).willReturn(Optional.empty());

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);
        given(boardingSessionRepository.getReferenceById(sessionId)).willReturn(session);

        Pets pet = new Pets();
        pet.setId(petId);
        given(petRepository.getReferenceById(petId)).willReturn(pet);

        Users staff = new Users();
        staff.setId(staffId);
        given(userRepository.getReferenceById(staffId)).willReturn(staff);

        CareLog savedLog = new CareLog();
        savedLog.setId(UUID.randomUUID());
        savedLog.setSession(session);
        savedLog.setPet(pet);
        savedLog.setStaff(staff);

        given(careLogRepository.save(any(CareLog.class))).willReturn(savedLog);
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        boardingStaffService.upsertCareLog(staffId, request);

        ArgumentCaptor<CareLog> captor = ArgumentCaptor.forClass(CareLog.class);
        verify(careLogRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthNote()).isNull();
        assertThat(captor.getValue().getStaffNote()).isNull();
    }

    private static CareLogRepository.SessionContextRow sessionContext(UUID sessionId, UUID petId, String status) {
        return new CareLogRepository.SessionContextRow() {
            @Override
            public UUID getSessionId() {
                return sessionId;
            }

            @Override
            public UUID getPetId() {
                return petId;
            }

            @Override
            public String getStatus() {
                return status;
            }
        };
    }

    private static CareLogRepository.StaffActiveStayRow staffStayRow(
            UUID sessionId,
            UUID petId,
            String petName,
            String roomLabel,
            Object checkinDate,
            Object expCheckin,
            Object expCheckout,
            String todayPeriods) {
        return new CareLogRepository.StaffActiveStayRow() {
            @Override
            public UUID getSessionId() {
                return sessionId;
            }

            @Override
            public UUID getPetId() {
                return petId;
            }

            @Override
            public String getPetName() {
                return petName;
            }

            @Override
            public String getRoomLabel() {
                return roomLabel;
            }

            @Override
            public Object getCheckinDate() {
                return checkinDate;
            }

            @Override
            public Object getExpCheckin() {
                return expCheckin;
            }

            @Override
            public Object getExpCheckout() {
                return expCheckout;
            }

            @Override
            public String getTodayPeriods() {
                return todayPeriods;
            }
        };
    }
}
