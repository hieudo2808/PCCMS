package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.request.CareLogCreateRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.RoomAvailabilityResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CareLogMedia;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.boarding.mapper.BoardingMapper;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.boarding.repository.CareLogMediaRepository;
import com.astral.express.pccms.boarding.repository.CareLogRepository;
import com.astral.express.pccms.boarding.repository.RoomAllocationRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.boarding.service.BoardingService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.dto.UploadedFileResponse;
import com.astral.express.pccms.filemedia.entity.FileAsset;
import com.astral.express.pccms.filemedia.entity.FileLink;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.filemedia.repository.FileLinkRepository;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.room.entity.Room;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardingService {

    private static final List<BoardingStatus> OWNER_DUPLICATE_BLOCKING_STATUSES = List.of(
            BoardingStatus.RESERVED,
            BoardingStatus.CHECKED_IN,
            BoardingStatus.IN_STAY);

    private final SecurityContextService SecurityContextService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final BoardingBookingRepository boardingBookingRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final BoardingSessionRepository boardingSessionRepository;
    private final CareLogRepository careLogRepository;
    private final CareLogMediaRepository careLogMediaRepository;
    private final FileMediaService fileMediaService;
    private final FileAssetRepository fileAssetRepository;
    private final FileLinkRepository fileLinkRepository;
    private final BillingHandoffService billingHandoffService;
    private final InvoiceRepository invoiceRepository;
    private final BoardingMapper boardingMapper;
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
        ensureOwnerBookingNotDuplicated(
                owner.getId(),
                pet.getId(),
                request.expectedCheckinAt(),
                request.expectedCheckoutAt());

        Long estimatedPrice = calculateAmount(
                request.expectedCheckinAt(),
                request.expectedCheckoutAt(),
                roomType.getBaseDailyPriceVnd());
        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setOrderCode(generateCode("SO"));
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
                .bookingCode(generateCode("BRD"))
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
public PageResponse<BoardingBookingResponse> listBookings(BoardingStatus statusCode, Pageable pageable) {
        if (statusCode == null) {
            return PageResponse.of(boardingBookingRepository.findAllByOrderByExpectedCheckinAtAsc(pageable)
                    .map(this::toBookingResponse));
        }
        return PageResponse.of(boardingBookingRepository.findByStatusCodeOrderByExpectedCheckinAtAsc(statusCode, pageable)
                .map(this::toBookingResponse));
    }
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
        if (roomAllocationRepository.existsActiveConflict(
                room.getId(),
                RoomAllocationStatus.ALLOCATED,
                booking.getExpectedCheckinAt(),
                booking.getExpectedCheckoutAt())) {
            throw new BusinessException(ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
        }

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

        Long finalAmount = calculateAmount(
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
        Optional<RoomAllocation> allocation = roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED);
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
        return toBookingResponse(booking, allocation.orElse(null), session);
    }
@Transactional
    public CareLogResponse createCareLog(UUID sessionId, CareLogCreateRequest request, List<MultipartFile> images) {
        Users staff = findUser(requireCurrentUserId());
        BoardingSession session = boardingSessionRepository.findWithDetailsById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND));
        if (session.getStatusCode() != BoardingStatus.CHECKED_IN && session.getStatusCode() != BoardingStatus.IN_STAY) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
        }
        if (careLogRepository.existsBySessionIdAndLogDateAndPeriodCode(sessionId, request.logDate(), request.periodCode())) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_004_CARE_LOG_DUPLICATED);
        }

        CareLog careLog = careLogRepository.save(CareLog.builder()
                .session(session)
                .pet(session.getBooking().getPet())
                .staff(staff)
                .logDate(request.logDate())
                .periodCode(request.periodCode())
                .feedingStatus(request.feedingStatus())
                .hygieneStatus(request.hygieneStatus())
                .healthNote(request.healthNote())
                .staffNote(request.staffNote())
                .build());
        List<CareLogMedia> media = saveCareLogMedia(careLog, request.caption(), images == null ? Collections.emptyList() : images);
        return boardingMapper.toCareLogResponse(careLog, media);
    }
public List<CareLogResponse> listCareLogs(UUID bookingId) {
        BoardingBooking booking = findBooking(bookingId);
        assertCanAccessBooking(booking);
        return careLogRepository.findBySessionBookingIdOrderByLogDateDescCreatedAtDesc(bookingId).stream()
                .map(careLog -> boardingMapper.toCareLogResponse(careLog, careLogMediaRepository.findByCareLogId(careLog.getId())))
                .toList();
    }

    private List<CareLogMedia> saveCareLogMedia(CareLog careLog, String caption, List<MultipartFile> images) {
        return images.stream()
                .map(image -> {
                    UploadedFileResponse uploadedFile = fileMediaService.uploadOwnerVisibleImage(image, requireCurrentUserId());
                    FileAsset fileAsset = fileAssetRepository.findById(uploadedFile.id())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ERR_FILE_001_INVALID_IMAGE));
                    fileLinkRepository.save(FileLink.builder()
                            .file(fileAsset)
                            .entityType("CARE_LOG")
                            .entityId(careLog.getId())
                            .purpose("CARE_LOG_MEDIA")
                            .build());
                    return careLogMediaRepository.save(CareLogMedia.builder()
                            .careLog(careLog)
                            .file(fileAsset)
                            .caption(caption)
                            .build());
                })
                .toList();
    }

    private BoardingBookingResponse toBookingResponse(BoardingBooking booking) {
        RoomAllocation allocation = roomAllocationRepository
                .findFirstByBookingIdAndStatusCode(booking.getId(), RoomAllocationStatus.ALLOCATED)
                .orElse(null);
        BoardingSession session = boardingSessionRepository.findByBookingId(booking.getId()).orElse(null);
        return toBookingResponse(booking, allocation, session);
    }

    private BoardingBookingResponse toBookingResponse(BoardingBooking booking, RoomAllocation allocation, BoardingSession session) {
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
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void assertCanAccessBooking(BoardingBooking booking) {
        if (SecurityContextService.hasAnyRole("ADMIN", "STAFF")) {
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

    private void validateTimeRange(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
        }
    }

    private void ensureOwnerBookingNotDuplicated(UUID ownerId, UUID petId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (boardingBookingRepository.existsOwnerPetBookingConflict(
                ownerId,
                petId,
                OWNER_DUPLICATE_BLOCKING_STATUSES,
                startAt,
                endAt)) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_006_DUPLICATE_BOOKING);
        }
    }

    private Long calculateAmount(OffsetDateTime startAt, OffsetDateTime endAt, Long unitPrice) {
        long billableDays = calculateBillableDays(startAt, endAt);
        return billableDays * unitPrice;
    }

    private long calculateBillableDays(OffsetDateTime startAt, OffsetDateTime endAt) {
        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes <= 0) {
            return 1;
        }
        long oneDayMinutes = 24L * 60L;
        return Math.max(1, (minutes + oneDayMinutes - 1) / oneDayMinutes);
    }

    private String generateCode(String prefix) {
        return prefix + "-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}



