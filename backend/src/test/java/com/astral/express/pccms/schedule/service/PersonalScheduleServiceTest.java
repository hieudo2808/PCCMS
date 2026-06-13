package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PersonalScheduleServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private SecurityContextService SecurityContextService;

    @InjectMocks
    private PersonalScheduleService personalScheduleService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/personal-schedule.csv", numLinesToSkip = 1)
    void should_followPersonalScheduleCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        PersonalScheduleCsvInput csv = parseInput(input);
        PageRequest pageable = PageRequest.of(0, 20);

        switch (scenario) {
            case "Current user views own schedule success", "Filter personal schedule by date range success" ->
                    assertListSuccess(csv, pageable);
            case "No personal schedule returns empty result" -> assertEmptySuccess(csv, pageable);
            case "Invalid date range rejected", "Unauthenticated request rejected",
                    "User cannot view another staff schedule" ->
                    assertFailure(scenario, csv, errorCode(expectedErrorCode), pageable);
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    private void assertListSuccess(PersonalScheduleCsvInput csv, PageRequest pageable) {
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        given(workScheduleRepository.findByStaffIdAndWorkDateBetween(CURRENT_USER_ID, csv.fromDate(), csv.toDate(), pageable))
                .willReturn(new PageImpl<>(List.of(schedule()), pageable, 1));

        PageResponse<WorkScheduleResponse> response = personalScheduleService.getMySchedules(
                csv.fromDate(), csv.toDate(), pageable);

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().getFirst().staffId()).isEqualTo(CURRENT_USER_ID);
    }

    private void assertEmptySuccess(PersonalScheduleCsvInput csv, PageRequest pageable) {
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        given(workScheduleRepository.findByStaffIdAndWorkDateBetween(CURRENT_USER_ID, csv.fromDate(), csv.toDate(), pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<WorkScheduleResponse> response = personalScheduleService.getMySchedules(
                csv.fromDate(), csv.toDate(), pageable);

        assertThat(response.data().content()).isEmpty();
    }

    private void assertFailure(
            String scenario,
            PersonalScheduleCsvInput csv,
            ErrorCode errorCode,
            PageRequest pageable) {
        if ("Unauthenticated request rejected".equals(scenario)) {
            given(SecurityContextService.getCurrentUserId()).willReturn(null);
        } else if ("User cannot view another staff schedule".equals(scenario)
                || "Invalid date range rejected".equals(scenario)) {
            given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        }

        if ("User cannot view another staff schedule".equals(scenario)) {
            assertThatThrownBy(() -> personalScheduleService.getStaffSchedules(
                    csv.staffId(), csv.fromDate(), csv.toDate(), pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", errorCode);
            return;
        }

        assertThatThrownBy(() -> personalScheduleService.getMySchedules(csv.fromDate(), csv.toDate(), pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private WorkSchedule schedule() {
        Users staff = new Users();
        staff.setId(CURRENT_USER_ID);
        staff.setFullName("Staff");
        Shift shift = new Shift();
        shift.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shift.setCode("MORNING");
        shift.setName("Morning");
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));

        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        schedule.setStaff(staff);
        schedule.setWorkDate(LocalDate.parse("2026-04-12"));
        schedule.setShift(shift);
        schedule.setCapacity(1);
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        return schedule;
    }

    private PersonalScheduleCsvInput parseInput(String input) {
        return new PersonalScheduleCsvInput(
                uuid(input, "staffId"),
                date(input, "fromDate"),
                date(input, "toDate")
        );
    }

    private ErrorCode errorCode(String expectedErrorCode) {
        if ("ERR_UNAUTHORIZED".equals(expectedErrorCode)) {
            return ErrorCode.ERR_401_UNAUTHORIZED;
        }
        return ErrorCode.valueOf(expectedErrorCode);
    }

    private LocalDate date(String input, String key) {
        String value = text(input, key);
        return value == null ? null : LocalDate.parse(value);
    }

    private UUID uuid(String input, String key) {
        String value = text(input, key);
        if (value == null) {
            return null;
        }
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", Long.parseLong(value)));
    }

    private String text(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                String value = pair[1].trim();
                return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
            }
        }
        return null;
    }

    private record PersonalScheduleCsvInput(UUID staffId, LocalDate fromDate, LocalDate toDate) {
    }

    @org.junit.jupiter.api.Test
    void should_GetStaffSchedules_Success() {
        UUID staffId = CURRENT_USER_ID;
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().plusDays(1);
        PageRequest pageable = PageRequest.of(0, 10);
        
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(workScheduleRepository.findByStaffIdAndWorkDateBetween(staffId, fromDate, toDate, pageable))
                .willReturn(new PageImpl<>(List.of(schedule()), pageable, 1));

        PageResponse<WorkScheduleResponse> response = personalScheduleService.getStaffSchedules(staffId, fromDate, toDate, pageable);
        
        assertThat(response.data().content()).hasSize(1);
    }
}

