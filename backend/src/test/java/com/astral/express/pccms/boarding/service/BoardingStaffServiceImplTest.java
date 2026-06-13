package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.UpsertCareLogRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardingStaffServiceImplTest {

    @Mock
    private CareLogRepository careLogRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BoardingStaffServiceImpl boardingStaffService;

    @Test
    void should_ListActiveStays_Success() {
        // GIVEN
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Object[] row = {
                sessionId, petId, "Milo", "Room 101", 
                java.sql.Date.valueOf(LocalDate.now().minusDays(2)), null, java.sql.Date.valueOf(LocalDate.now().plusDays(2)), "MORNING,NOON"
        };
        given(careLogRepository.findActiveStaysForStaff()).willReturn(java.util.Collections.singletonList(row));

        // WHEN
        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).petName()).isEqualTo("Milo");
        assertThat(responses.get(0).todayLogSummary()).contains("Sáng", "Trưa");
    }

    @Test
    void should_ListSessionLogs_Success() {
        // GIVEN
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        Object[] contextRow = {sessionId, petId, "IN_STAY"};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));

        Pets pet = new Pets();
        pet.setId(petId);
        pet.setName("Milo");

        Users staff = new Users();
        staff.setId(UUID.randomUUID());
        staff.setFullName("Staff A");

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);

        CareLog log1 = new CareLog();
        log1.setId(UUID.randomUUID());
        log1.setSession(session);
        log1.setPet(pet);
        log1.setStaff(staff);
        log1.setPeriodCode(CarePeriod.MORNING);
        log1.setFeedingStatus("GOOD");
        log1.setHygieneStatus("CLEAN");
        
        given(careLogRepository.findBySessionIdAndLogDateOrderByPeriodCodeDesc(eq(sessionId), any(LocalDate.class)))
                .willReturn(List.of(log1));
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        // WHEN
        List<CareLogResponse> responses = boardingStaffService.listSessionLogs(sessionId, LocalDate.now());

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).periodCode()).isEqualTo(CarePeriod.MORNING);
    }

    @Test
    void should_ThrowException_when_ListSessionLogs_SessionNotActive() {
        // GIVEN
        UUID sessionId = UUID.randomUUID();
        Object[] contextRow = {sessionId, UUID.randomUUID(), "COMPLETED"};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));

        // WHEN & THEN
        assertThatThrownBy(() -> boardingStaffService.listSessionLogs(sessionId, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BRG_002_SESSION_NOT_ACTIVE);
    }

    @Test
    void should_UpsertCareLog_Success() {
        // GIVEN
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        UpsertCareLogRequest request = new UpsertCareLogRequest(sessionId, LocalDate.now(), "MORNING", "GOOD", "CLEAN", "All good", null);
        Object[] contextRow = {sessionId, petId, "IN_STAY"};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));
        
        given(careLogRepository.findBySessionIdAndLogDateAndPeriodCode(eq(sessionId), any(LocalDate.class), eq(CarePeriod.MORNING)))
                .willReturn(Optional.empty());

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);
        given(entityManager.getReference(BoardingSession.class, sessionId)).willReturn(session);
        
        Pets pet = new Pets();
        pet.setId(petId);
        given(entityManager.getReference(Pets.class, petId)).willReturn(pet);
        
        Users staff = new Users();
        staff.setId(staffId);
        staff.setFullName("Staff B");
        given(entityManager.getReference(Users.class, staffId)).willReturn(staff);

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

        // WHEN
        CareLogResponse response = boardingStaffService.upsertCareLog(staffId, request);

        // THEN
        assertThat(response.feedingStatus()).isEqualTo("GOOD");
        assertThat(response.periodCode()).isEqualTo(CarePeriod.MORNING);
        verify(careLogRepository).save(any(CareLog.class));
    }

    @Test
    void listSessionLogs_shouldUseNow_whenLogDateIsNull() {
        UUID sessionId = UUID.randomUUID();
        Object[] contextRow = {sessionId, UUID.randomUUID(), "IN_STAY"};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));
        given(careLogRepository.findBySessionIdAndLogDateOrderByPeriodCodeDesc(eq(sessionId), any(LocalDate.class)))
                .willReturn(List.of());

        List<CareLogResponse> responses = boardingStaffService.listSessionLogs(sessionId, null);
        assertThat(responses).isEmpty();
    }

    @Test
    void listActiveStays_shouldHandleNullsAndVariousDateTypes() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        // row: sessionId, petId, petName(null), roomLabel(null), checkinDate1(null), checkinDate2(Date), checkoutDate(Timestamp), todayPeriods(null)
        Object[] row = {
                sessionId, petId, null, null, 
                null, new java.util.Date(), new java.sql.Timestamp(System.currentTimeMillis()), null
        };
        given(careLogRepository.findActiveStaysForStaff()).willReturn(java.util.Collections.singletonList(row));

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
        
        Object[] row = {
                sessionId, petId, "Milo", "Room 101", 
                java.sql.Date.valueOf(LocalDate.now()), null, null, "MORNING"
        };
        given(careLogRepository.findActiveStaysForStaff()).willReturn(java.util.Collections.singletonList(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses.get(0).todayLogSummary()).isEqualTo("Đã cập nhật sáng");
    }

    @Test
    void listActiveStays_shouldHandleAllPeriodsLogged() {
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        Object[] row = {
                sessionId, petId, "Milo", "Room 101", 
                java.sql.Date.valueOf(LocalDate.now()), null, null, "MORNING,NOON,AFTERNOON"
        };
        given(careLogRepository.findActiveStaysForStaff()).willReturn(java.util.Collections.singletonList(row));

        List<StaffBoardingStayResponse> responses = boardingStaffService.listActiveStays();

        assertThat(responses.get(0).todayLogSummary()).isEqualTo("Đã cập nhật đủ 3 buổi");
    }

    @Test
    void assertActiveSession_shouldHandleNullStatus() {
        UUID sessionId = UUID.randomUUID();
        Object[] contextRow = {sessionId, UUID.randomUUID(), null};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));

        assertThatThrownBy(() -> boardingStaffService.listSessionLogs(sessionId, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BRG_002_SESSION_NOT_ACTIVE);
    }

    @Test
    void upsertCareLog_shouldTrimEmptyHealthNoteToNull() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        
        UpsertCareLogRequest request = new UpsertCareLogRequest(sessionId, null, "MORNING", "GOOD", "CLEAN", "   ", "   ");
        Object[] contextRow = {sessionId, petId, "IN_STAY"};
        given(careLogRepository.findSessionContext(sessionId)).willReturn(Optional.of(contextRow));
        
        given(careLogRepository.findBySessionIdAndLogDateAndPeriodCode(eq(sessionId), any(LocalDate.class), eq(CarePeriod.MORNING)))
                .willReturn(Optional.empty());

        BoardingSession session = new BoardingSession();
        session.setId(sessionId);
        given(entityManager.getReference(BoardingSession.class, sessionId)).willReturn(session);
        
        Pets pet = new Pets();
        pet.setId(petId);
        given(entityManager.getReference(Pets.class, petId)).willReturn(pet);
        
        Users staff = new Users();
        staff.setId(staffId);
        given(entityManager.getReference(Users.class, staffId)).willReturn(staff);

        CareLog savedLog = new CareLog();
        savedLog.setId(UUID.randomUUID());
        savedLog.setSession(session);
        savedLog.setPet(pet);
        savedLog.setStaff(staff);
        
        given(careLogRepository.save(any(CareLog.class))).willReturn(savedLog);
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        CareLogResponse response = boardingStaffService.upsertCareLog(staffId, request);
        
        org.mockito.ArgumentCaptor<CareLog> captor = org.mockito.ArgumentCaptor.forClass(CareLog.class);
        verify(careLogRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthNote()).isNull();
        assertThat(captor.getValue().getStaffNote()).isNull();
    }

}
