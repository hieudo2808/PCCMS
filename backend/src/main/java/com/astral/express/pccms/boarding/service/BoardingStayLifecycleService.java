package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.boarding.mapper.BoardingMapper;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.boarding.repository.RoomAllocationRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.BusinessNotificationService;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardingStayLifecycleService {

    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final BoardingBookingRepository boardingBookingRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final BoardingSessionRepository boardingSessionRepository;
    private final BillingHandoffService billingHandoffService;
    private final InvoiceRepository invoiceRepository;
    private final BoardingMapper boardingMapper;
    private final BoardingPricingPolicy boardingPricingPolicy;
    private final BoardingAvailabilityPolicy boardingAvailabilityPolicy;
    private final BusinessNotificationService businessNotificationService;

    @Transactional
    public BoardingBookingResponse confirmBooking(UUID bookingId, BoardingConfirmRequest request) {
        Users staff = findUser(requireCurrentUserId());
        BoardingBooking booking = findBooking(bookingId);
        requireStatus(booking, BoardingStatus.RESERVED);
        if (roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED).isPresent()) {
            throw new BusinessException(ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
        }

        Room room = roomRepository.findAvailableByIdWithLock(request.roomId(), RoomStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND));
        if (!room.getRoomType().getId().equals(booking.getRequestedRoomType().getId())) {
            throw new BusinessException(ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
        }
        boardingAvailabilityPolicy.requireRoomAvailable(
                room.getId(),
                booking.getExpectedCheckinAt(),
                booking.getExpectedCheckoutAt()
        );

        RoomAllocation allocation = roomAllocationRepository.save(RoomAllocation.builder()
                .booking(booking)
                .room(room)
                .allocatedFrom(booking.getExpectedCheckinAt())
                .allocatedTo(booking.getExpectedCheckoutAt())
                .allocatedBy(staff)
                .statusCode(RoomAllocationStatus.ALLOCATED)
                .build());
        BoardingSession session = boardingSessionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND));
        session.setRoomAllocation(allocation);
        booking.getServiceOrder().setStatusCode(ServiceOrderStatus.CONFIRMED);
        booking.getServiceOrder().setUpdatedBy(staff.getId());
        boardingSessionRepository.save(session);
        serviceOrderRepository.save(booking.getServiceOrder());
        return toBookingResponse(booking, allocation, session);
    }

    @Transactional
    public BoardingBookingResponse checkIn(UUID bookingId) {
        Users staff = findUser(requireCurrentUserId());
        BoardingBooking booking = findBooking(bookingId);
        requireStatus(booking, BoardingStatus.RESERVED);
        RoomAllocation allocation = requireAllocation(bookingId);
        BoardingSession session = requireSession(bookingId);
        OffsetDateTime now = OffsetDateTime.now();

        booking.setStatusCode(BoardingStatus.CHECKED_IN);
        session.setStatusCode(BoardingStatus.CHECKED_IN);
        session.setActualCheckinAt(now);
        session.setCheckedInBy(staff);
        booking.getServiceOrder().setStatusCode(ServiceOrderStatus.IN_PROGRESS);
        booking.getServiceOrder().setActualStartAt(now);
        booking.getServiceOrder().setUpdatedBy(staff.getId());
        allocation.getRoom().setStatusCode(RoomStatus.OCCUPIED);
        roomRepository.save(allocation.getRoom());
        serviceOrderRepository.save(booking.getServiceOrder());
        boardingBookingRepository.save(booking);
        boardingSessionRepository.save(session);
        businessNotificationService.boardingCheckedIn(
                booking.getOwner().getId(), booking.getId(), booking.getPet().getName());
        return toBookingResponse(booking, allocation, session);
    }

    @Transactional
    public BoardingBookingResponse startStay(UUID bookingId) {
        BoardingBooking booking = findBooking(bookingId);
        requireStatus(booking, BoardingStatus.CHECKED_IN);
        BoardingSession session = requireSession(bookingId);
        RoomAllocation allocation = requireAllocation(bookingId);
        booking.setStatusCode(BoardingStatus.IN_STAY);
        session.setStatusCode(BoardingStatus.IN_STAY);
        boardingBookingRepository.save(booking);
        boardingSessionRepository.save(session);
        return toBookingResponse(booking, allocation, session);
    }

    @Transactional
    public BoardingBookingResponse checkOut(UUID bookingId) {
        Users staff = findUser(requireCurrentUserId());
        BoardingBooking booking = findBooking(bookingId);
        if (booking.getStatusCode() != BoardingStatus.CHECKED_IN && booking.getStatusCode() != BoardingStatus.IN_STAY) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
        }
        BoardingSession session = requireSession(bookingId);
        RoomAllocation allocation = requireAllocation(bookingId);
        OffsetDateTime now = OffsetDateTime.now();

        booking.setStatusCode(BoardingStatus.CHECKED_OUT);
        session.setStatusCode(BoardingStatus.CHECKED_OUT);
        session.setActualCheckoutAt(now);
        session.setCheckedOutBy(staff);
        allocation.setReleasedAt(now);
        allocation.setStatusCode(RoomAllocationStatus.RELEASED);
        if (allocation.getRoom().getStatusCode() == RoomStatus.OCCUPIED) {
            allocation.getRoom().setStatusCode(RoomStatus.AVAILABLE);
            roomRepository.save(allocation.getRoom());
        }

        Long finalAmount = boardingPricingPolicy.calculateAmount(
                Optional.ofNullable(session.getActualCheckinAt()).orElse(booking.getExpectedCheckinAt()),
                now,
                booking.getRequestedRoomType().getBaseDailyPriceVnd());
        booking.getServiceOrder().setStatusCode(ServiceOrderStatus.COMPLETED);
        booking.getServiceOrder().setCompletedAt(now);
        booking.getServiceOrder().setFinalAmountVnd(finalAmount);
        booking.getServiceOrder().setUpdatedBy(staff.getId());
        serviceOrderRepository.save(booking.getServiceOrder());
        roomAllocationRepository.save(allocation);
        boardingBookingRepository.save(booking);
        boardingSessionRepository.save(session);
        Invoice invoice = billingHandoffService.createBoardingInvoice(booking, session, staff);
        businessNotificationService.boardingCheckedOut(
                booking.getOwner().getId(), booking.getId(), booking.getPet().getName());
        return boardingMapper.toBookingResponse(booking, allocation, session, invoice);
    }

    @Transactional
    public BoardingBookingResponse cancelBooking(UUID bookingId, BoardingCancelRequest request) {
        Users actor = findUser(requireCurrentUserId());
        BoardingBooking booking = findBooking(bookingId);
        assertCanAccessBooking(booking);
        if (booking.getStatusCode() == BoardingStatus.CHECKED_OUT || booking.getStatusCode() == BoardingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
        }
        Optional<RoomAllocation> allocation = roomAllocationRepository.findFirstByBookingIdAndStatusCode(
                bookingId,
                RoomAllocationStatus.ALLOCATED);
        allocation.ifPresent(roomAllocation -> {
            roomAllocation.setStatusCode(RoomAllocationStatus.CANCELLED);
            roomAllocation.setReleasedAt(OffsetDateTime.now());
            roomAllocationRepository.save(roomAllocation);
        });
        BoardingSession session = requireSession(bookingId);
        booking.setStatusCode(BoardingStatus.CANCELLED);
        session.setStatusCode(BoardingStatus.CANCELLED);
        booking.getServiceOrder().setStatusCode(ServiceOrderStatus.CANCELLED);
        booking.getServiceOrder().setCancelledAt(OffsetDateTime.now());
        booking.getServiceOrder().setCancellationReason(request.reason());
        booking.getServiceOrder().setUpdatedBy(actor.getId());
        serviceOrderRepository.save(booking.getServiceOrder());
        boardingBookingRepository.save(booking);
        boardingSessionRepository.save(session);
        businessNotificationService.boardingCancelled(
                booking.getOwner().getId(), booking.getId(), booking.getPet().getName());
        return toBookingResponse(booking, allocation.orElse(null), session);
    }

    private BoardingBookingResponse toBookingResponse(
            BoardingBooking booking,
            RoomAllocation allocation,
            BoardingSession session) {
        Invoice invoice = invoiceRepository.findByServiceOrderId(booking.getServiceOrder().getId()).orElse(null);
        return boardingMapper.toBookingResponse(booking, allocation, session, invoice);
    }

    private BoardingBooking findBooking(UUID bookingId) {
        return boardingBookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_001_BOOKING_NOT_FOUND));
    }

    private BoardingSession requireSession(UUID bookingId) {
        return boardingSessionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND));
    }

    private RoomAllocation requireAllocation(UUID bookingId) {
        return roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE));
    }

    private Users findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void assertCanAccessBooking(BoardingBooking booking) {
        if (securityContextService.hasAnyRole("ADMIN", "STAFF")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        if (!booking.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }

    private void requireStatus(BoardingBooking booking, BoardingStatus expectedStatus) {
        if (booking.getStatusCode() != expectedStatus) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
        }
    }
}
