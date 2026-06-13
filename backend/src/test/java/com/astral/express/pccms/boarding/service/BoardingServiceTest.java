package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
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
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.filemedia.repository.FileAssetRepository;
import com.astral.express.pccms.filemedia.repository.FileLinkRepository;
import com.astral.express.pccms.filemedia.service.FileMediaService;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomRepository;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.CareLogCreateRequest;
import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import com.astral.express.pccms.billing.entity.Invoice;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Collections;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardingServiceTest {

    @Mock
    private SecurityContextService SecurityContextService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private BoardingBookingRepository boardingBookingRepository;

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private BoardingSessionRepository boardingSessionRepository;

    @Mock
    private CareLogRepository careLogRepository;

    @Mock
    private CareLogMediaRepository careLogMediaRepository;

    @Mock
    private FileMediaService fileMediaService;

    @Mock
    private FileAssetRepository fileAssetRepository;

    @Mock
    private FileLinkRepository fileLinkRepository;

    @Mock
    private BillingHandoffService billingHandoffService;

    @Mock
    private InvoiceRepository invoiceRepository;

    private BoardingService boardingService;

    @BeforeEach
    void setUp() {
        boardingService = new BoardingService(
                SecurityContextService,
                userRepository,
                petRepository,
                roomTypeRepository,
                roomRepository,
                serviceCatalogRepository,
                serviceOrderRepository,
                boardingBookingRepository,
                roomAllocationRepository,
                boardingSessionRepository,
                careLogRepository,
                careLogMediaRepository,
                fileMediaService,
                fileAssetRepository,
                fileLinkRepository,
                billingHandoffService,
                invoiceRepository,
                new BoardingMapper());
    }

    @ParameterizedTest(name = "[{0}] {1}: {6}")
    @CsvFileSource(resources = "/testcases/boarding-service-lifecycle.csv", numLinesToSkip = 1)
    void should_enforce_boarding_booking_rules(
            String ruleId,
            String caseId,
            String action,
            String mockState,
            String expectedResult,
            String expectedError,
            String note) {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        OffsetDateTime checkinAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime checkoutAt = checkinAt.plusDays(2);

        Users owner = Users.builder().id(ownerId).fullName("Demo Owner").build();
        Pets pet = Pets.builder().id(petId).name("Milu").owner(owner).build();
        RoomType roomType = RoomType.builder()
                .id(roomTypeId)
                .code("STANDARD")
                .name("Phòng thường")
                .baseDailyPriceVnd(150000L)
                .isActive(true)
                .build();
        ServiceCatalog serviceCatalog = ServiceCatalog.builder()
                .id(UUID.randomUUID())
                .serviceCode("BRD-STAY")
                .name("Lưu trú theo ngày")
                .categoryCode(ServiceCategory.BOARDING)
                .basePriceVnd(150000L)
                .isActive(true)
                .build();

        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(roomTypeRepository.findByIdAndIsActiveTrue(roomTypeId)).willReturn(Optional.of(roomType));
        given(serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrueOrderByCreatedAtDesc(ServiceCategory.BOARDING))
                .willReturn(Optional.of(serviceCatalog));
        given(boardingBookingRepository.existsOwnerPetBookingConflict(eq(ownerId), eq(petId), anyCollection(), eq(checkinAt), eq(checkoutAt)))
                .willReturn("DUPLICATE_BOOKING".equals(mockState));

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> execute(action, petId, roomTypeId, checkinAt, checkoutAt))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedError));
            verify(serviceOrderRepository, never()).save(any(ServiceOrder.class));
            verify(boardingBookingRepository, never()).save(any(BoardingBooking.class));
            return;
        }

        given(serviceOrderRepository.save(any(ServiceOrder.class))).willAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            assertThat(order.getRequestedAt()).isNotNull();
            assertThat(order.getStatusCode()).isEqualTo(ServiceOrderStatus.REQUESTED);
            order.setId(UUID.randomUUID());
            return order;
        });
        given(boardingBookingRepository.save(any(BoardingBooking.class))).willAnswer(invocation -> {
            BoardingBooking booking = invocation.getArgument(0);
            booking.setId(UUID.randomUUID());
            return booking;
        });
        given(boardingSessionRepository.save(any(BoardingSession.class))).willAnswer(invocation -> {
            BoardingSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });
        given(invoiceRepository.findByServiceOrderId(any(UUID.class))).willReturn(Optional.empty());

        var response = execute(action, petId, roomTypeId, checkinAt, checkoutAt);

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(BoardingStatus.RESERVED);
        assertThat(response.estimatedPriceVnd()).isEqualTo(300000L);
        assertThat(response.petId()).isEqualTo(petId);
        assertThat(response.requestedRoomTypeId()).isEqualTo(roomTypeId);
    }

    private com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse execute(
            String action,
            UUID petId,
            UUID roomTypeId,
            OffsetDateTime checkinAt,
            OffsetDateTime checkoutAt) {
        return switch (action) {
            case "CREATE_BOOKING" -> boardingService.createBooking(new BoardingBookingCreateRequest(
                    petId,
                    roomTypeId,
                    checkinAt,
                    checkoutAt,
                    "Cho ăn hạt riêng"));
            default -> throw new IllegalArgumentException("Unsupported action " + action);
        };
    }

    private BoardingBooking createMockBooking(UUID bookingId, BoardingStatus status) {
        Users owner = Users.builder().id(UUID.randomUUID()).build();
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(owner).build();
        ServiceOrder order = new ServiceOrder();
        order.setId(UUID.randomUUID());
        RoomType roomType = RoomType.builder().id(UUID.randomUUID()).baseDailyPriceVnd(100L).build();
        return BoardingBooking.builder()
                .id(bookingId)
                .owner(owner)
                .pet(pet)
                .serviceOrder(order)
                .statusCode(status)
                .requestedRoomType(roomType)
                .expectedCheckinAt(java.time.OffsetDateTime.now())
                .expectedCheckoutAt(java.time.OffsetDateTime.now().plusDays(1))
                .build();
    }

    @org.junit.jupiter.api.Test
    void should_getAvailability() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusDays(2);
        given(roomTypeRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(List.of(new RoomType()));
        var res = boardingService.getAvailability(start, end);
        assertThat(res).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_listMyBookings() {
        UUID userId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(userId);
        Page<BoardingBooking> page = new PageImpl<>(List.of(createMockBooking(UUID.randomUUID(), BoardingStatus.RESERVED)));
        given(boardingBookingRepository.findByOwnerIdOrderByExpectedCheckinAtDesc(eq(userId), any())).willReturn(page);
        
        var res = boardingService.listMyBookings(PageRequest.of(0, 10));
        assertThat(res.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_listBookings() {
        Page<BoardingBooking> page = new PageImpl<>(List.of(createMockBooking(UUID.randomUUID(), BoardingStatus.RESERVED)));
        given(boardingBookingRepository.findByStatusCodeOrderByExpectedCheckinAtAsc(eq(BoardingStatus.RESERVED), any())).willReturn(page);
        
        var res = boardingService.listBookings(BoardingStatus.RESERVED, PageRequest.of(0, 10));
        assertThat(res.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void should_confirmBooking() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        
        Users staff = Users.builder().id(staffId).build();
        
        
        ServiceOrder order = new ServiceOrder();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        Room room = Room.builder().id(roomId).roomType(booking.getRequestedRoomType()).build();
                
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();

        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        given(roomRepository.findAvailableByIdWithLock(roomId, RoomStatus.AVAILABLE)).willReturn(Optional.of(room));
        given(roomAllocationRepository.existsActiveConflict(any(), any(), any(), any())).willReturn(false);
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));
        
        given(roomAllocationRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(roomId));
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.RESERVED);
    }

    @org.junit.jupiter.api.Test
    void should_checkIn() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        
        ServiceOrder order = new ServiceOrder();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        booking.setOwner(staff);
                
        Room room = Room.builder().roomType(booking.getRequestedRoomType()).build();
        RoomAllocation allocation = RoomAllocation.builder().room(room).build();
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();

        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.of(allocation));
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));

        var res = boardingService.checkIn(bookingId);
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.CHECKED_IN);
    }

    @org.junit.jupiter.api.Test
    void should_startStay() {
        UUID bookingId = UUID.randomUUID();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.CHECKED_IN);
                
        RoomAllocation allocation = RoomAllocation.builder().room(Room.builder().id(UUID.randomUUID()).build()).build();
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();

        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.of(allocation));

        var res = boardingService.startStay(bookingId);
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.IN_STAY);
    }

    @org.junit.jupiter.api.Test
    void should_checkOut() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        
        ServiceOrder order = new ServiceOrder();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.IN_STAY);
                
        Room room = Room.builder().statusCode(RoomStatus.OCCUPIED).roomType(booking.getRequestedRoomType()).build();
        RoomAllocation allocation = RoomAllocation.builder().room(room).build();
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).actualCheckinAt(OffsetDateTime.now().minusDays(1)).build();

        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.of(allocation));
        given(billingHandoffService.createBoardingInvoice(any(), any(), any())).willReturn(new Invoice());

        var res = boardingService.checkOut(bookingId);
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.CHECKED_OUT);
    }

    @org.junit.jupiter.api.Test
    void should_cancelBooking() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        
        ServiceOrder order = new ServiceOrder();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        booking.setOwner(staff);
                
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();

        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));

        var res = boardingService.cancelBooking(bookingId, new BoardingCancelRequest("Bận"));
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.CANCELLED);
    }

    @org.junit.jupiter.api.Test
    void should_createCareLog() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Users staff = Users.builder().id(staffId).build();
        
        ServiceOrder order = new ServiceOrder();
        BoardingBooking booking = createMockBooking(UUID.randomUUID(), BoardingStatus.IN_STAY);
                
        BoardingSession session = BoardingSession.builder()
                .id(sessionId)
                .statusCode(BoardingStatus.IN_STAY)
                .booking(booking)
                .build();

        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(staff));
        given(boardingSessionRepository.findWithDetailsById(sessionId)).willReturn(Optional.of(session));
        given(careLogRepository.existsBySessionIdAndLogDateAndPeriodCode(any(), any(), any())).willReturn(false);
        given(careLogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var res = boardingService.createCareLog(sessionId, new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "GOOD", "CLEAN", "Health", "Staff Note", "Caption"), Collections.emptyList());
        assertThat(res.periodCode()).isEqualTo(CarePeriod.MORNING);
    }

    @org.junit.jupiter.api.Test
    void should_listCareLogs() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Users owner = Users.builder().id(userId).build();
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.IN_STAY);
        booking.setOwner(owner);
        booking.getPet().setOwner(owner);

        given(SecurityContextService.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(owner));
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(careLogRepository.findBySessionBookingIdOrderByLogDateDescCreatedAtDesc(bookingId)).willReturn(List.of(CareLog.builder()
                .id(UUID.randomUUID())
                .session(BoardingSession.builder().id(UUID.randomUUID()).build())
                .staff(Users.builder().id(UUID.randomUUID()).build())
                .build()));

        var res = boardingService.listCareLogs(bookingId);
        assertThat(res).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void createBooking_shouldThrowPetNotFound() {
        UUID ownerId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(Users.builder().id(ownerId).build()));
        given(petRepository.findById(any())).willReturn(Optional.empty());

        BoardingBookingCreateRequest request = new BoardingBookingCreateRequest(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), "Note");
        assertThatThrownBy(() -> boardingService.createBooking(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_PET_001_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void createBooking_shouldThrowForbiddenIfPetNotOwned() {
        UUID ownerId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(Users.builder().id(ownerId).build()));
        
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(Users.builder().id(UUID.randomUUID()).build()).build();
        given(petRepository.findById(any())).willReturn(Optional.of(pet));

        BoardingBookingCreateRequest request = new BoardingBookingCreateRequest(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), "Note");
        assertThatThrownBy(() -> boardingService.createBooking(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @org.junit.jupiter.api.Test
    void createBooking_shouldThrowRoomTypeNotFound() {
        UUID ownerId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(Users.builder().id(ownerId).build()));
        
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(Users.builder().id(ownerId).build()).build();
        given(petRepository.findById(any())).willReturn(Optional.of(pet));
        given(roomTypeRepository.findByIdAndIsActiveTrue(any())).willReturn(Optional.empty());

        BoardingBookingCreateRequest request = new BoardingBookingCreateRequest(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), "Note");
        assertThatThrownBy(() -> boardingService.createBooking(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_001_ROOM_TYPE_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void createBooking_shouldThrowCatalogNotFound() {
        UUID ownerId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(Users.builder().id(ownerId).build()));
        
        Pets pet = Pets.builder().id(UUID.randomUUID()).owner(Users.builder().id(ownerId).build()).build();
        given(petRepository.findById(any())).willReturn(Optional.of(pet));
        given(roomTypeRepository.findByIdAndIsActiveTrue(any())).willReturn(Optional.of(new RoomType()));
        given(serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrueOrderByCreatedAtDesc(any())).willReturn(Optional.empty());

        BoardingBookingCreateRequest request = new BoardingBookingCreateRequest(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), "Note");
        assertThatThrownBy(() -> boardingService.createBooking(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_404_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void listBookings_shouldHandleNullStatusCode() {
        Page<BoardingBooking> page = new PageImpl<>(List.of(createMockBooking(UUID.randomUUID(), BoardingStatus.RESERVED)));
        given(boardingBookingRepository.findAllByOrderByExpectedCheckinAtAsc(any())).willReturn(page);
        var res = boardingService.listBookings(null, PageRequest.of(0, 10));
        assertThat(res.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void confirmBooking_shouldThrowIfAllocationExists() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED))
                .willReturn(Optional.of(new RoomAllocation()));

        assertThatThrownBy(() -> boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(UUID.randomUUID())))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void confirmBooking_shouldThrowIfRoomNotFound() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        given(roomRepository.findAvailableByIdWithLock(any(), any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(UUID.randomUUID())))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_002_ROOM_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void confirmBooking_shouldThrowIfRoomTypeMismatch() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        
        Room room = Room.builder().id(UUID.randomUUID()).roomType(RoomType.builder().id(UUID.randomUUID()).build()).build();
        given(roomRepository.findAvailableByIdWithLock(any(), any())).willReturn(Optional.of(room));

        assertThatThrownBy(() -> boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(UUID.randomUUID())))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void confirmBooking_shouldThrowIfConflictExists() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        
        Room room = Room.builder().id(UUID.randomUUID()).roomType(booking.getRequestedRoomType()).build();
        given(roomRepository.findAvailableByIdWithLock(any(), any())).willReturn(Optional.of(room));
        given(roomAllocationRepository.existsActiveConflict(any(), any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(UUID.randomUUID())))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void confirmBooking_shouldThrowIfSessionNotFound() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.empty());
        
        Room room = Room.builder().id(UUID.randomUUID()).roomType(booking.getRequestedRoomType()).build();
        given(roomRepository.findAvailableByIdWithLock(any(), any())).willReturn(Optional.of(room));
        given(roomAllocationRepository.existsActiveConflict(any(), any(), any(), any())).willReturn(false);
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.empty());
        given(roomAllocationRepository.save(any())).willAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> boardingService.confirmBooking(bookingId, new BoardingConfirmRequest(UUID.randomUUID())))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void checkOut_shouldThrowIfInvalidStatus() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));

        assertThatThrownBy(() -> boardingService.checkOut(bookingId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
    }

    @org.junit.jupiter.api.Test
    void checkOut_shouldHandleRoomNotOccupied() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.IN_STAY);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        
        Room room = Room.builder().statusCode(RoomStatus.MAINTENANCE).roomType(booking.getRequestedRoomType()).build();
        RoomAllocation allocation = RoomAllocation.builder().room(room).build();
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.of(allocation));
        
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));
        given(billingHandoffService.createBoardingInvoice(any(), any(), any())).willReturn(new Invoice());

        var res = boardingService.checkOut(bookingId);
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.CHECKED_OUT);
    }

    @org.junit.jupiter.api.Test
    void cancelBooking_shouldThrowIfCheckedOutOrCancelled() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.CHECKED_OUT);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));

        assertThatThrownBy(() -> boardingService.cancelBooking(bookingId, new BoardingCancelRequest("reason")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
    }

    @org.junit.jupiter.api.Test
    void cancelBooking_shouldHandleAllocationPresent() {
        UUID staffId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(true);
        
        BoardingBooking booking = createMockBooking(bookingId, BoardingStatus.RESERVED);
        given(boardingBookingRepository.findWithDetailsById(bookingId)).willReturn(Optional.of(booking));
        
        RoomAllocation allocation = RoomAllocation.builder().room(new Room()).build();
        given(roomAllocationRepository.findFirstByBookingIdAndStatusCode(bookingId, RoomAllocationStatus.ALLOCATED)).willReturn(Optional.of(allocation));
        
        BoardingSession session = BoardingSession.builder().id(UUID.randomUUID()).build();
        given(boardingSessionRepository.findByBookingId(bookingId)).willReturn(Optional.of(session));

        var res = boardingService.cancelBooking(bookingId, new BoardingCancelRequest("reason"));
        assertThat(res.statusCode()).isEqualTo(BoardingStatus.CANCELLED);
    }

    @org.junit.jupiter.api.Test
    void createCareLog_shouldThrowIfSessionNotFound() {
        UUID staffId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        given(boardingSessionRepository.findWithDetailsById(any())).willReturn(Optional.empty());

        CareLogCreateRequest request = new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "G", "C", "H", "S", "C");
        assertThatThrownBy(() -> boardingService.createCareLog(UUID.randomUUID(), request, null))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_005_SESSION_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void createCareLog_shouldThrowIfInvalidStatus() {
        UUID staffId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingSession session = BoardingSession.builder().statusCode(BoardingStatus.RESERVED).build();
        given(boardingSessionRepository.findWithDetailsById(any())).willReturn(Optional.of(session));

        CareLogCreateRequest request = new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "G", "C", "H", "S", "C");
        assertThatThrownBy(() -> boardingService.createCareLog(UUID.randomUUID(), request, null))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_003_INVALID_STATUS_TRANSITION);
    }

    @org.junit.jupiter.api.Test
    void createCareLog_shouldHandleNullImages() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(UUID.randomUUID(), BoardingStatus.IN_STAY);
        BoardingSession session = BoardingSession.builder().id(sessionId).statusCode(BoardingStatus.IN_STAY).booking(booking).build();
        given(boardingSessionRepository.findWithDetailsById(any())).willReturn(Optional.of(session));
        given(careLogRepository.existsBySessionIdAndLogDateAndPeriodCode(any(), any(), any())).willReturn(false);
        given(careLogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        CareLogCreateRequest request = new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "G", "C", "H", "S", "C");
        var res = boardingService.createCareLog(sessionId, request, null);
        assertThat(res.periodCode()).isEqualTo(CarePeriod.MORNING);
    }

    @org.junit.jupiter.api.Test
    void createCareLog_shouldSaveMedia() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(UUID.randomUUID(), BoardingStatus.IN_STAY);
        BoardingSession session = BoardingSession.builder().id(sessionId).statusCode(BoardingStatus.IN_STAY).booking(booking).build();
        given(boardingSessionRepository.findWithDetailsById(any())).willReturn(Optional.of(session));
        given(careLogRepository.existsBySessionIdAndLogDateAndPeriodCode(any(), any(), any())).willReturn(false);
        given(careLogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        com.astral.express.pccms.filemedia.dto.UploadedFileResponse uploadRes = new com.astral.express.pccms.filemedia.dto.UploadedFileResponse(UUID.randomUUID(), "url", "key", "image/jpeg", 100L);
        given(fileMediaService.uploadOwnerVisibleImage(any(), any())).willReturn(uploadRes);
        given(fileAssetRepository.findById(any())).willReturn(Optional.of(com.astral.express.pccms.filemedia.entity.FileAsset.builder().build()));
        given(careLogMediaRepository.save(any())).willAnswer(i -> {
            com.astral.express.pccms.boarding.entity.CareLogMedia media = i.getArgument(0);
            media.setId(UUID.randomUUID());
            return media;
        });

        CareLogCreateRequest request = new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "G", "C", "H", "S", "C");
        var res = boardingService.createCareLog(sessionId, request, List.of(file));
        assertThat(res.periodCode()).isEqualTo(CarePeriod.MORNING);
    }
    
    @org.junit.jupiter.api.Test
    void createCareLog_shouldThrowIfMediaNotFound() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(userRepository.findById(staffId)).willReturn(Optional.of(Users.builder().id(staffId).build()));
        
        BoardingBooking booking = createMockBooking(UUID.randomUUID(), BoardingStatus.IN_STAY);
        BoardingSession session = BoardingSession.builder().id(sessionId).statusCode(BoardingStatus.IN_STAY).booking(booking).build();
        given(boardingSessionRepository.findWithDetailsById(any())).willReturn(Optional.of(session));
        given(careLogRepository.existsBySessionIdAndLogDateAndPeriodCode(any(), any(), any())).willReturn(false);
        given(careLogRepository.save(any())).willAnswer(i -> i.getArgument(0));

        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        com.astral.express.pccms.filemedia.dto.UploadedFileResponse uploadRes = new com.astral.express.pccms.filemedia.dto.UploadedFileResponse(UUID.randomUUID(), "url", "key", "image/jpeg", 100L);
        given(fileMediaService.uploadOwnerVisibleImage(any(), any())).willReturn(uploadRes);
        given(fileAssetRepository.findById(any())).willReturn(Optional.empty());

        CareLogCreateRequest request = new CareLogCreateRequest(java.time.LocalDate.now(), CarePeriod.MORNING, "G", "C", "H", "S", "C");
        
        assertThatThrownBy(() -> boardingService.createCareLog(sessionId, request, List.of(file)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_FILE_001_INVALID_IMAGE);
    }

    @org.junit.jupiter.api.Test
    void requireCurrentUserId_shouldThrowIfNull() {
        given(SecurityContextService.getCurrentUserId()).willReturn(null);
        assertThatThrownBy(() -> boardingService.listMyBookings(PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @org.junit.jupiter.api.Test
    void assertCanAccessBooking_shouldThrowForbidden() {
        UUID staffId = UUID.randomUUID();
        given(SecurityContextService.getCurrentUserId()).willReturn(staffId);
        given(SecurityContextService.hasAnyRole("ADMIN", "STAFF")).willReturn(false);
        
        BoardingBooking booking = createMockBooking(UUID.randomUUID(), BoardingStatus.RESERVED); // owner id is random, not staffId

        assertThatThrownBy(() -> {
            try {
                java.lang.reflect.Method method = BoardingService.class.getDeclaredMethod("assertCanAccessBooking", BoardingBooking.class);
                method.setAccessible(true);
                method.invoke(boardingService, booking);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        })
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @org.junit.jupiter.api.Test
    void calculateBillableDays_shouldReturn1IfNegative() {
        BoardingBookingCreateRequest request = new BoardingBookingCreateRequest(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now().minusDays(1), "Note");
        assertThatThrownBy(() -> boardingService.createBooking(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
    }
    
    @org.junit.jupiter.api.Test
    void getAvailability_shouldThrowIfInvalidTimeRange() {
        assertThatThrownBy(() -> boardingService.getAvailability(OffsetDateTime.now(), OffsetDateTime.now().minusDays(1)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
            
        assertThatThrownBy(() -> boardingService.getAvailability(null, OffsetDateTime.now()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
            
        assertThatThrownBy(() -> boardingService.getAvailability(OffsetDateTime.now(), null))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_002_INVALID_TIME_RANGE);
    }

}
