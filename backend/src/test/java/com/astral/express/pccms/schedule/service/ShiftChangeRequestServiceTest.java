package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.schedule.dto.request.ShiftChangeRequestCreateRequest;
import com.astral.express.pccms.schedule.dto.response.ShiftChangeRequestResponse;
import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.ShiftChangeRequest;
import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import com.astral.express.pccms.schedule.repository.ShiftChangeRequestRepository;
import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShiftChangeRequestServiceTest {

    @Mock
    private ShiftChangeRequestRepository shiftChangeRequestRepository;
    @Mock
    private WorkScheduleRepository workScheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private ShiftChangeRequestService service;

    @Test
    void should_GetMyRequests_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findByRequestedByIdAndStatusCode(eq(userId), eq(ShiftRequestStatus.PENDING), any())).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getMyRequests(ShiftRequestStatus.PENDING, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_GetAdminRequests_Success() {
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findByStatusCode(eq(ShiftRequestStatus.PENDING), any())).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getAdminRequests(ShiftRequestStatus.PENDING, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_GetIncomingRequests_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findByTargetStaffIdAndStatusCode(eq(userId), eq(ShiftRequestStatus.PENDING), any())).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getIncomingRequests(ShiftRequestStatus.PENDING, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_CreateRequest_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        Users staff = new Users();
        staff.setId(userId);
        schedule.setStaff(staff);
        
        when(workScheduleRepository.findById(any())).thenReturn(Optional.of(schedule));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(schedule.getId(), "Reason", null);
        ShiftChangeRequestResponse res = service.createRequest(request);
        
        assertThat(res).isNotNull();
        assertThat(res.reason()).isEqualTo("Reason");
    }

    @Test
    void should_CancelOwnRequest_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        Users user = new Users();
        user.setId(userId);
        req.setRequestedBy(user);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.cancelOwnRequest(req.getId());
        
        assertThat(res).isNotNull();
        assertThat(req.getStatusCode()).isEqualTo(ShiftRequestStatus.CANCELLED);
    }
    
    @Test
    void should_RespondToRequest_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        Users target = new Users();
        target.setId(userId);
        req.setTargetStaff(target);
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        com.astral.express.pccms.schedule.entity.Shift shift = new com.astral.express.pccms.schedule.entity.Shift();
        shift.setId(UUID.randomUUID());
        schedule.setShift(shift);
        req.setSchedule(schedule);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(any())).thenReturn(Optional.of(target));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.respondToRequest(req.getId(), ShiftRequestStatus.ACCEPTED);
        
        assertThat(res).isNotNull();
        assertThat(req.getStatusCode()).isEqualTo(ShiftRequestStatus.ACCEPTED);
    }


    @Test
    void should_ThrowException_when_CreateRequestScheduleStaffIsNull() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setStaff(null);
        
        when(workScheduleRepository.findById(any())).thenReturn(Optional.of(schedule));
        
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(UUID.randomUUID(), "Reason", null);
        assertThatThrownBy(() -> service.createRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ThrowException_when_CreateRequestScheduleDateInPast() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        WorkSchedule schedule = new WorkSchedule();
        Users staff = new Users();
        staff.setId(userId);
        schedule.setStaff(staff);
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setWorkDate(LocalDate.now().minusDays(1)); // Past date
        
        when(workScheduleRepository.findById(any())).thenReturn(Optional.of(schedule));
        
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(UUID.randomUUID(), "Reason", null);
        assertThatThrownBy(() -> service.createRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_when_ValidateTargetStaffCanTakeSchedule_RoleMismatch() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        Users target = new Users();
        target.setId(userId);
        
        com.astral.express.pccms.user.entity.Roles targetRole = new com.astral.express.pccms.user.entity.Roles();
        targetRole.setId(UUID.randomUUID());
        target.setRole(targetRole);
        req.setTargetStaff(target);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        com.astral.express.pccms.user.entity.Roles scheduleRole = new com.astral.express.pccms.user.entity.Roles();
        scheduleRole.setId(UUID.randomUUID()); // different role
        schedule.setRole(scheduleRole);
        
        com.astral.express.pccms.schedule.entity.Shift shift = new com.astral.express.pccms.schedule.entity.Shift();
        shift.setId(UUID.randomUUID());
        schedule.setShift(shift);
        req.setSchedule(schedule);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(any())).thenReturn(Optional.of(target));
        
        assertThatThrownBy(() -> service.respondToRequest(req.getId(), ShiftRequestStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_UpdateRequestStatus_AcceptedWithTarget() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        
        Users target = new Users();
        target.setId(UUID.randomUUID());
        req.setTargetStaff(target);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        com.astral.express.pccms.schedule.entity.Shift shift = new com.astral.express.pccms.schedule.entity.Shift();
        shift.setId(UUID.randomUUID());
        schedule.setShift(shift);
        req.setSchedule(schedule);
        
        Users resolver = new Users();
        resolver.setId(userId);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(userId)).thenReturn(Optional.of(resolver));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(false);
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.updateRequestStatus(req.getId(), ShiftRequestStatus.ACCEPTED);
        
        assertThat(res).isNotNull();
        assertThat(req.getStatusCode()).isEqualTo(ShiftRequestStatus.ACCEPTED);
        assertThat(schedule.getStaff().getId()).isEqualTo(target.getId());
    }
    @Test
    void should_ThrowException_when_CancelOwnRequestNotOwned() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        Users owner = new Users();
        owner.setId(UUID.randomUUID()); // different user
        req.setRequestedBy(owner);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        
        assertThatThrownBy(() -> service.cancelOwnRequest(req.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ThrowException_when_CancelOwnRequestNotPending() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.ACCEPTED); // Not pending
        Users user = new Users();
        user.setId(userId);
        req.setRequestedBy(user);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        
        assertThatThrownBy(() -> service.cancelOwnRequest(req.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_when_UpdateRequestStatusNotPending() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.ACCEPTED); // Not pending
        
        Users resolver = new Users();
        resolver.setId(userId);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(userId)).thenReturn(Optional.of(resolver));
        
        assertThatThrownBy(() -> service.updateRequestStatus(req.getId(), ShiftRequestStatus.REJECTED))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_UpdateRequestStatus_AcceptedNoTargetStaff() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        req.setTargetStaff(null); // No target staff
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        req.setSchedule(schedule);
        
        Users resolver = new Users();
        resolver.setId(userId);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(userId)).thenReturn(Optional.of(resolver));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.updateRequestStatus(req.getId(), ShiftRequestStatus.ACCEPTED);
        
        assertThat(res).isNotNull();
        assertThat(req.getStatusCode()).isEqualTo(ShiftRequestStatus.ACCEPTED);
        assertThat(schedule.getStatusCode()).isEqualTo(ScheduleStatus.CANCELLED); // Should be cancelled
    }

    @Test
    void should_ThrowException_when_RespondToRequestTargetIsNull() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setTargetStaff(null); // No target staff
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        
        assertThatThrownBy(() -> service.respondToRequest(req.getId(), ShiftRequestStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ThrowException_when_RespondToRequestNotPending() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.ACCEPTED); // Not pending
        Users target = new Users();
        target.setId(userId);
        req.setTargetStaff(target);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        
        assertThatThrownBy(() -> service.respondToRequest(req.getId(), ShiftRequestStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_when_RespondToRequestInvalidAction() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        Users target = new Users();
        target.setId(userId);
        req.setTargetStaff(target);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        
        assertThatThrownBy(() -> service.respondToRequest(req.getId(), ShiftRequestStatus.CANCELLED)) // Invalid action
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void createRequest_withTargetStaffNull() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        UUID scheduleId = UUID.randomUUID();
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(scheduleId, "Reason", null);
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(scheduleId);
        Users staff = new Users();
        staff.setId(userId);
        schedule.setStaff(staff);
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        
        when(workScheduleRepository.findById(scheduleId)).thenReturn(java.util.Optional.of(schedule));
        when(shiftChangeRequestRepository.existsByScheduleIdAndRequestedByIdAndStatusCode(scheduleId, userId, ShiftRequestStatus.PENDING)).thenReturn(false);
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.createRequest(request);
        assertThat(res).isNotNull();
    }

    @Test
    void cancelOwnRequest_shouldThrow_whenRequestedByNull() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        UUID requestId = UUID.randomUUID();
        ShiftChangeRequest request = new ShiftChangeRequest();
        request.setRequestedBy(null);
        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(java.util.Optional.of(request));
        
        assertThatThrownBy(() -> service.cancelOwnRequest(requestId))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }
    
    @Test
    void updateRequestStatus_shouldHandleRejected() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        UUID requestId = UUID.randomUUID();
        ShiftChangeRequest request = new ShiftChangeRequest();
        request.setStatusCode(ShiftRequestStatus.PENDING);
        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(java.util.Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(new Users()));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.updateRequestStatus(requestId, ShiftRequestStatus.REJECTED);
        assertThat(res.statusCode()).isEqualTo(ShiftRequestStatus.REJECTED);
    }

    @Test
    void respondToRequest_shouldThrow_whenActionInvalid() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        UUID requestId = UUID.randomUUID();
        ShiftChangeRequest request = new ShiftChangeRequest();
        Users targetStaff = new Users();
        targetStaff.setId(userId);
        request.setTargetStaff(targetStaff);
        request.setStatusCode(ShiftRequestStatus.PENDING);
        
        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(java.util.Optional.of(request));
        
        assertThatThrownBy(() -> service.respondToRequest(requestId, ShiftRequestStatus.CANCELLED))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void respondToRequest_shouldHandleRejected() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        UUID requestId = UUID.randomUUID();
        ShiftChangeRequest request = new ShiftChangeRequest();
        Users targetStaff = new Users();
        targetStaff.setId(userId);
        request.setTargetStaff(targetStaff);
        request.setStatusCode(ShiftRequestStatus.PENDING);
        
        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(java.util.Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(new Users()));
        when(shiftChangeRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        ShiftChangeRequestResponse res = service.respondToRequest(requestId, ShiftRequestStatus.REJECTED);
        assertThat(res.statusCode()).isEqualTo(ShiftRequestStatus.REJECTED);
    }
    
    @Test
    void validateTargetStaffCanTakeSchedule_shouldThrow_whenTargetRoleNull() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        UUID requestId = UUID.randomUUID();
        ShiftChangeRequest request = new ShiftChangeRequest();
        Users targetStaff = new Users();
        targetStaff.setId(userId);
        request.setTargetStaff(targetStaff);
        request.setStatusCode(ShiftRequestStatus.PENDING);
        
        WorkSchedule schedule = new WorkSchedule();
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setId(UUID.randomUUID());
        schedule.setRole(role);
        request.setSchedule(schedule);
        
        when(shiftChangeRequestRepository.findById(requestId)).thenReturn(java.util.Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(targetStaff));
        
        assertThatThrownBy(() -> service.respondToRequest(requestId, ShiftRequestStatus.ACCEPTED))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }



    @Test
    void should_GetMyRequests_NullStatusCode_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findByRequestedById(eq(userId), any())).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getMyRequests(null, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_GetAdminRequests_NullStatusCode_Success() {
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getAdminRequests(null, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_GetIncomingRequests_NullStatusCode_Success() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        ShiftChangeRequest req = new ShiftChangeRequest();
        Page<ShiftChangeRequest> page = new PageImpl<>(List.of(req));
        when(shiftChangeRequestRepository.findByTargetStaffId(eq(userId), any())).thenReturn(page);
        
        PageResponse<ShiftChangeRequestResponse> res = service.getIncomingRequests(null, PageRequest.of(0, 10));
        assertThat(res).isNotNull();
    }

    @Test
    void should_ThrowException_WhenCreateRequest_TargetStaffIsSelf() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        Users staff = new Users();
        staff.setId(userId);
        schedule.setStaff(staff);
        
        when(workScheduleRepository.findById(any())).thenReturn(Optional.of(schedule));
        
        Users targetStaff = new Users();
        targetStaff.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetStaff));
        
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(schedule.getId(), "Reason", userId);
        assertThatThrownBy(() -> service.createRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_WhenCreateRequest_AlreadyPending() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setStatusCode(ScheduleStatus.ASSIGNED);
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        Users staff = new Users();
        staff.setId(userId);
        schedule.setStaff(staff);
        
        when(workScheduleRepository.findById(any())).thenReturn(Optional.of(schedule));
        when(shiftChangeRequestRepository.existsByScheduleIdAndRequestedByIdAndStatusCode(any(), any(), any())).thenReturn(true);
        
        ShiftChangeRequestCreateRequest request = new ShiftChangeRequestCreateRequest(schedule.getId(), "Reason", null);
        assertThatThrownBy(() -> service.createRequest(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_WhenValidateTargetStaffCanTakeSchedule_Conflict() {
        UUID userId = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        
        ShiftChangeRequest req = new ShiftChangeRequest();
        req.setId(UUID.randomUUID());
        req.setStatusCode(ShiftRequestStatus.PENDING);
        Users target = new Users();
        target.setId(userId);
        req.setTargetStaff(target);
        
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkDate(LocalDate.now().plusDays(2));
        com.astral.express.pccms.schedule.entity.Shift shift = new com.astral.express.pccms.schedule.entity.Shift();
        shift.setId(UUID.randomUUID());
        schedule.setShift(shift);
        req.setSchedule(schedule);
        
        when(shiftChangeRequestRepository.findById(any())).thenReturn(Optional.of(req));
        when(userRepository.findById(any())).thenReturn(Optional.of(target));
        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(any(), any(), any())).thenReturn(true);
        
        assertThatThrownBy(() -> service.respondToRequest(req.getId(), ShiftRequestStatus.ACCEPTED))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_WhenCurrentUserIdIsNull() {
        when(securityContextService.getCurrentUserId()).thenReturn(null);
        assertThatThrownBy(() -> service.getMyRequests(null, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

}