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
import com.astral.express.pccms.schedule.service.ShiftChangeRequestService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftChangeRequestService {
    private final ShiftChangeRequestRepository shiftChangeRequestRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final UserRepository userRepository;
    private final SecurityContextService SecurityContextService;
@PreAuthorize("isAuthenticated()")
    public PageResponse<ShiftChangeRequestResponse> getMyRequests(
            ShiftRequestStatus statusCode,
            Pageable pageable) {
        UUID currentUserId = currentUserId();
        Page<ShiftChangeRequest> requests = statusCode == null
                ? shiftChangeRequestRepository.findByRequestedById(currentUserId, pageable)
                : shiftChangeRequestRepository.findByRequestedByIdAndStatusCode(currentUserId, statusCode, pageable);
        return PageResponse.of(requests.map(ScheduleMapperSupport::toShiftChangeRequestResponse));
    }
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public PageResponse<ShiftChangeRequestResponse> getAdminRequests(
            ShiftRequestStatus statusCode,
            Pageable pageable) {
        Page<ShiftChangeRequest> requests = statusCode == null
                ? shiftChangeRequestRepository.findAll(pageable)
                : shiftChangeRequestRepository.findByStatusCode(statusCode, pageable);
        return PageResponse.of(requests.map(ScheduleMapperSupport::toShiftChangeRequestResponse));
    }

    @PreAuthorize("isAuthenticated()")
    public PageResponse<ShiftChangeRequestResponse> getIncomingRequests(
            ShiftRequestStatus statusCode,
            Pageable pageable) {
        UUID currentUserId = currentUserId();
        Page<ShiftChangeRequest> requests = statusCode == null
                ? shiftChangeRequestRepository.findByTargetStaffId(currentUserId, pageable)
                : shiftChangeRequestRepository.findByTargetStaffIdAndStatusCode(currentUserId, statusCode, pageable);
        return PageResponse.of(requests.map(ScheduleMapperSupport::toShiftChangeRequestResponse));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ShiftChangeRequestResponse createRequest(ShiftChangeRequestCreateRequest request) {
        UUID currentUserId = currentUserId();
        WorkSchedule schedule = workScheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        validateScheduleCanBeChanged(schedule, currentUserId);
        Users targetStaff = findTargetStaff(request.targetStaffId());
        if (targetStaff != null && currentUserId.equals(targetStaff.getId())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (shiftChangeRequestRepository.existsByScheduleIdAndRequestedByIdAndStatusCode(
                request.scheduleId(), currentUserId, ShiftRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        ShiftChangeRequest changeRequest = new ShiftChangeRequest();
        changeRequest.setSchedule(schedule);
        changeRequest.setRequestedBy(schedule.getStaff());
        changeRequest.setTargetStaff(targetStaff);
        changeRequest.setReason(request.reason());
        changeRequest.setStatusCode(ShiftRequestStatus.PENDING);
        return ScheduleMapperSupport.toShiftChangeRequestResponse(shiftChangeRequestRepository.save(changeRequest));
    }
@Transactional
    @PreAuthorize("isAuthenticated()")
    public ShiftChangeRequestResponse cancelOwnRequest(UUID requestId) {
        UUID currentUserId = currentUserId();
        ShiftChangeRequest request = shiftChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        if (request.getRequestedBy() == null || !currentUserId.equals(request.getRequestedBy().getId())) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (request.getStatusCode() != ShiftRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        request.setStatusCode(ShiftRequestStatus.CANCELLED);
        return ScheduleMapperSupport.toShiftChangeRequestResponse(shiftChangeRequestRepository.save(request));
    }
@Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ShiftChangeRequestResponse updateRequestStatus(UUID requestId, ShiftRequestStatus statusCode) {
        UUID currentUserId = currentUserId();
        ShiftChangeRequest request = shiftChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        Users resolver = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));

        if (request.getStatusCode() != ShiftRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        request.setStatusCode(statusCode);
        request.setResolvedBy(resolver);
        request.setResolvedAt(OffsetDateTime.now());

        if (statusCode == ShiftRequestStatus.ACCEPTED) {
            WorkSchedule schedule = request.getSchedule();
            if (request.getTargetStaff() != null) {
                validateTargetStaffCanTakeSchedule(schedule, request.getTargetStaff());
                schedule.setStaff(request.getTargetStaff());
            } else {
                schedule.setStatusCode(ScheduleStatus.CANCELLED);
            }
            workScheduleRepository.save(schedule);
        }

        return ScheduleMapperSupport.toShiftChangeRequestResponse(shiftChangeRequestRepository.save(request));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ShiftChangeRequestResponse respondToRequest(UUID requestId, ShiftRequestStatus action) {
        UUID currentUserId = currentUserId();
        ShiftChangeRequest request = shiftChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));

        if (request.getTargetStaff() == null || !currentUserId.equals(request.getTargetStaff().getId())) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        if (request.getStatusCode() != ShiftRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        if (action != ShiftRequestStatus.ACCEPTED && action != ShiftRequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        Users resolver = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));

        request.setStatusCode(action);
        request.setResolvedBy(resolver);
        request.setResolvedAt(OffsetDateTime.now());

        if (action == ShiftRequestStatus.ACCEPTED) {
            validateTargetStaffCanTakeSchedule(request.getSchedule(), request.getTargetStaff());
            WorkSchedule schedule = request.getSchedule();
            schedule.setStaff(request.getTargetStaff());
            workScheduleRepository.save(schedule);
        }

        return ScheduleMapperSupport.toShiftChangeRequestResponse(shiftChangeRequestRepository.save(request));
    }

    private void validateScheduleCanBeChanged(WorkSchedule schedule, UUID currentUserId) {
        if (schedule.getStaff() == null || !currentUserId.equals(schedule.getStaff().getId())) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (schedule.getStatusCode() != ScheduleStatus.ASSIGNED || !schedule.getWorkDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void validateTargetStaffCanTakeSchedule(WorkSchedule schedule, Users targetStaff) {
        if (schedule.getRole() != null) {
            boolean hasRole = targetStaff.getRole() != null &&
                    targetStaff.getRole().getId().equals(schedule.getRole().getId());
            if (!hasRole) {
                throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
            }
        }
        boolean conflict = workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(
                targetStaff.getId(), schedule.getWorkDate(), schedule.getShift().getId());
        if (conflict) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private Users findTargetStaff(UUID targetStaffId) {
        if (targetStaffId == null) {
            return null;
        }
        return userRepository.findById(targetStaffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private UUID currentUserId() {
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }
}


