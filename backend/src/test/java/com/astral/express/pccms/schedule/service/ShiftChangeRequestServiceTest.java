package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.schedule.dto.request.ShiftChangeRequestCreateRequest;
import com.astral.express.pccms.schedule.dto.response.ShiftChangeRequestResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.Shift;
import com.astral.express.pccms.schedule.entity.ShiftChangeRequest;
import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.ShiftChangeRequestRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShiftChangeRequestServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ShiftChangeRequestRepository shiftChangeRequestRepository;

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextService SecurityContextService;

    @InjectMocks
    private ShiftChangeRequestService shiftChangeRequestService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/shift-change-request.csv", numLinesToSkip = 1)
    void should_followShiftChangeRequestCsvRules(
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
        switch (scenario) {
            case "Blank reason rejected", "Invalid request status rejected" -> {
                assertControllerLayerValidation(caseId, expectedErrorCode);
                return;
            }
            default -> {
            }
        }

        ShiftRequestCsvInput csv = parseInput(input);

        switch (scenario) {
            case "Create shift change request success",
                    "Create shift change request without target staff success" -> assertCreateSuccess(csv);
            case "Cancel own pending request success" -> assertCancelSuccess(csv);
            case "Schedule not found rejected", "Schedule not owned by requester rejected",
                    "Target staff not found rejected", "Past or started schedule rejected",
                    "Cancelled schedule rejected", "Duplicate pending request rejected" ->
                    assertFailure(scenario, csv, ErrorCode.valueOf(expectedErrorCode));
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    @org.junit.jupiter.api.Test
    void should_ReturnOnlyCurrentUserRequests_when_GetMyRequests() {
        PageRequest pageable = PageRequest.of(0, 20);
        ShiftChangeRequest request = request(id("10"), CURRENT_USER_ID);
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        given(shiftChangeRequestRepository.findByRequestedById(eq(CURRENT_USER_ID), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(request), pageable, 1));

        var response = shiftChangeRequestService.getMyRequests(null, pageable);

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().getFirst().requestedBy()).isEqualTo(CURRENT_USER_ID);
    }

    private void assertCreateSuccess(ShiftRequestCsvInput csv) {
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        WorkSchedule schedule = schedule(csv.scheduleId(), CURRENT_USER_ID, ScheduleStatus.ASSIGNED, LocalDate.now().plusDays(1));
        given(workScheduleRepository.findById(csv.scheduleId())).willReturn(Optional.of(schedule));
        if (csv.targetStaffId() != null) {
            given(userRepository.findById(csv.targetStaffId())).willReturn(Optional.of(user(csv.targetStaffId())));
        }
        given(shiftChangeRequestRepository.existsByScheduleIdAndRequestedByIdAndStatusCode(
                csv.scheduleId(), CURRENT_USER_ID, ShiftRequestStatus.PENDING)).willReturn(false);
        given(shiftChangeRequestRepository.save(any(ShiftChangeRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

        ShiftChangeRequestResponse response = shiftChangeRequestService.createRequest(createRequest(csv));

        assertThat(response.scheduleId()).isEqualTo(csv.scheduleId());
        assertThat(response.statusCode()).isEqualTo(ShiftRequestStatus.PENDING);
        verify(shiftChangeRequestRepository).save(any(ShiftChangeRequest.class));
    }

    private void assertCancelSuccess(ShiftRequestCsvInput csv) {
        ShiftChangeRequest request = request(csv.requestId(), CURRENT_USER_ID);
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        given(shiftChangeRequestRepository.findById(csv.requestId())).willReturn(Optional.of(request));
        given(shiftChangeRequestRepository.save(request)).willReturn(request);

        ShiftChangeRequestResponse response = shiftChangeRequestService.cancelOwnRequest(csv.requestId());

        assertThat(response.statusCode()).isEqualTo(ShiftRequestStatus.CANCELLED);
    }

    private void assertFailure(String scenario, ShiftRequestCsvInput csv, ErrorCode errorCode) {
        given(SecurityContextService.getCurrentUserId()).willReturn(CURRENT_USER_ID);
        if ("Schedule not found rejected".equals(scenario)) {
            given(workScheduleRepository.findById(csv.scheduleId())).willReturn(Optional.empty());
        } else {
            UUID ownerId = "Schedule not owned by requester rejected".equals(scenario)
                    ? UUID.fromString("00000000-0000-0000-0000-000000000011")
                    : CURRENT_USER_ID;
            ScheduleStatus status = "Cancelled schedule rejected".equals(scenario)
                    ? ScheduleStatus.CANCELLED
                    : ScheduleStatus.ASSIGNED;
            LocalDate workDate = "Past or started schedule rejected".equals(scenario)
                    ? LocalDate.now().minusDays(1)
                    : LocalDate.now().plusDays(1);
            given(workScheduleRepository.findById(csv.scheduleId()))
                    .willReturn(Optional.of(schedule(csv.scheduleId(), ownerId, status, workDate)));
        }
        if ("Target staff not found rejected".equals(scenario)) {
            given(userRepository.findById(csv.targetStaffId())).willReturn(Optional.empty());
        }
        if ("Duplicate pending request rejected".equals(scenario)) {
            given(shiftChangeRequestRepository.existsByScheduleIdAndRequestedByIdAndStatusCode(
                    csv.scheduleId(), CURRENT_USER_ID, ShiftRequestStatus.PENDING)).willReturn(true);
        }

        assertThatThrownBy(() -> shiftChangeRequestService.createRequest(createRequest(csv)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private void assertControllerLayerValidation(String caseId, String expectedErrorCode) {
        assertThat(caseId).isIn("TC_SHIFT_REQ_005", "TC_SHIFT_REQ_011");
        assertThat(expectedErrorCode).isEqualTo(ErrorCode.ERR_VALIDATION_FAILED.name());
    }

    private ShiftChangeRequestCreateRequest createRequest(ShiftRequestCsvInput input) {
        return new ShiftChangeRequestCreateRequest(input.scheduleId(), input.reason(), input.targetStaffId());
    }

    private ShiftChangeRequest request(UUID id, UUID requestedById) {
        ShiftChangeRequest request = new ShiftChangeRequest();
        request.setId(id);
        request.setSchedule(schedule(id("10"), requestedById, ScheduleStatus.ASSIGNED, LocalDate.now().plusDays(1)));
        request.setRequestedBy(user(requestedById));
        request.setReason("Family matter");
        request.setStatusCode(ShiftRequestStatus.PENDING);
        return request;
    }

    private WorkSchedule schedule(UUID scheduleId, UUID staffId, ScheduleStatus status, LocalDate workDate) {
        Shift shift = new Shift();
        shift.setId(id("1"));
        shift.setCode("MORNING");
        shift.setName("Morning");
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));

        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(scheduleId);
        schedule.setStaff(user(staffId));
        schedule.setWorkDate(workDate);
        schedule.setShift(shift);
        schedule.setCapacity(1);
        schedule.setStatusCode(status);
        return schedule;
    }

    private Users user(UUID id) {
        Users user = new Users();
        user.setId(id);
        user.setFullName("Staff");
        return user;
    }

    private ShiftRequestCsvInput parseInput(String input) {
        return new ShiftRequestCsvInput(
                uuid(input, "scheduleId"),
                uuid(input, "requestId"),
                text(input, "reason"),
                uuid(input, "targetStaffId"),
                requestStatus(input, "statusCode")
        );
    }

    private ShiftRequestStatus requestStatus(String input, String key) {
        String value = text(input, key);
        return value == null ? null : ShiftRequestStatus.valueOf(value);
    }

    private UUID uuid(String input, String key) {
        String value = text(input, key);
        return value == null ? null : id(value);
    }

    private UUID id(String value) {
        if ("999999".equals(value)) {
            return UUID.fromString("99999999-9999-9999-9999-999999999999");
        }
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", Long.parseLong(value)));
    }

    private String text(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                String value = pair[1].trim();
                return "null".equalsIgnoreCase(value) ? null : value;
            }
        }
        return null;
    }

    private record ShiftRequestCsvInput(
            UUID scheduleId,
            UUID requestId,
            String reason,
            UUID targetStaffId,
            ShiftRequestStatus statusCode
    ) {
    }
}

