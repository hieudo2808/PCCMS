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
}




