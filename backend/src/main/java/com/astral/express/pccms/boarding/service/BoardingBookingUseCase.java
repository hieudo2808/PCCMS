package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.dto.response.RoomAvailabilityResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.mapper.BoardingMapper;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardingBookingUseCase {
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final BoardingBookingRepository boardingBookingRepository;
    private final BoardingSessionRepository boardingSessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final BoardingMapper boardingMapper;
    private final BoardingPricingPolicy boardingPricingPolicy;
    private final BoardingCodeGenerator boardingCodeGenerator;
    private final BoardingAvailabilityPolicy boardingAvailabilityPolicy;

    public List<RoomAvailabilityResponse> getAvailability(OffsetDateTime startAt, OffsetDateTime endAt) {
        validateTimeRange(startAt, endAt);
        return roomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(roomType -> new RoomAvailabilityResponse(
                        roomType.getId(),
                        roomType.getCode(),
                        roomType.getName(),
                        roomType.getDefaultCapacity(),
                        roomType.getBaseDailyPriceVnd(),
                        roomRepository.countAvailableByType(roomType.getId(), RoomStatus.AVAILABLE, startAt, endAt)))
                .toList();
    }

    @Transactional
    public BoardingBookingResponse createBooking(BoardingBookingCreateRequest request) {
        validateTimeRange(request.expectedCheckinAt(), request.expectedCheckoutAt());
        UUID currentUserId = requireCurrentUserId();
        Users owner = findUser(currentUserId);
        Pets pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
        if (!pet.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        RoomType roomType = roomTypeRepository.findByIdAndIsActiveTrue(request.roomTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND));
        ServiceCatalog serviceCatalog = serviceCatalogRepository
                .findFirstByCategoryCodeAndIsActiveTrueOrderByCreatedAtDesc(ServiceCategory.BOARDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
        boardingAvailabilityPolicy.requireOwnerBookingAvailable(
                owner.getId(),
                pet.getId(),
                request.expectedCheckinAt(),
                request.expectedCheckoutAt());

        Long estimatedPrice = boardingPricingPolicy.calculateAmount(
                request.expectedCheckinAt(),
                request.expectedCheckoutAt(),
                roomType.getBaseDailyPriceVnd());
        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setOrderCode(boardingCodeGenerator.generate("SO"));
        serviceOrder.setOwner(owner);
        serviceOrder.setPet(pet);
        serviceOrder.setService(serviceCatalog);
        serviceOrder.setCategoryCode(ServiceCategory.BOARDING);
        serviceOrder.setStatusCode(ServiceOrderStatus.REQUESTED);
        serviceOrder.setRequestedAt(OffsetDateTime.now());
        serviceOrder.setPlannedStartAt(request.expectedCheckinAt());
        serviceOrder.setPlannedEndAt(request.expectedCheckoutAt());
        serviceOrder.setBaseAmountVnd(estimatedPrice);
        serviceOrder.setCreatedBy(owner.getId());
        serviceOrder.setUpdatedBy(owner.getId());
        ServiceOrder savedServiceOrder = serviceOrderRepository.save(serviceOrder);

        BoardingBooking booking = BoardingBooking.builder()
                .bookingCode(boardingCodeGenerator.generate("BRD"))
                .serviceOrder(savedServiceOrder)
                .owner(owner)
                .pet(pet)
                .requestedRoomType(roomType)
                .expectedCheckinAt(request.expectedCheckinAt())
                .expectedCheckoutAt(request.expectedCheckoutAt())
                .specialCareRequest(request.specialCareRequest())
                .estimatedPriceVnd(estimatedPrice)
                .statusCode(BoardingStatus.RESERVED)
                .createdBy(owner)
                .build();
        BoardingBooking savedBooking = boardingBookingRepository.save(booking);
        BoardingSession session = boardingSessionRepository.save(BoardingSession.builder()
                .booking(savedBooking)
                .statusCode(BoardingStatus.RESERVED)
                .build());
        log.info("[BOARDING_CREATED] - {} - {} - {}", currentUserId, savedBooking.getId(), OffsetDateTime.now());
        return toBookingResponse(savedBooking, null, session);
    }

    public PageResponse<BoardingBookingResponse> listMyBookings(Pageable pageable) {
        UUID currentUserId = requireCurrentUserId();
        return PageResponse.of(boardingBookingRepository.findByOwnerIdOrderByExpectedCheckinAtDesc(currentUserId, pageable)
                .map(this::toBookingResponse));
    }

    private BoardingBookingResponse toBookingResponse(BoardingBooking booking) {
        BoardingSession session = boardingSessionRepository.findByBookingId(booking.getId()).orElse(null);
        return toBookingResponse(booking, null, session);
    }

    private BoardingBookingResponse toBookingResponse(
            BoardingBooking booking,
            RoomAllocation allocation,
            BoardingSession session) {
        Invoice invoice = invoiceRepository.findByServiceOrderId(booking.getServiceOrder().getId()).orElse(null);
        return boardingMapper.toBookingResponse(booking, allocation, session, invoice);
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

    private void validateTimeRange(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
        }
    }
}
