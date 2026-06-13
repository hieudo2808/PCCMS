package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateBoardingBookingRequest;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.appointment.dto.response.RoomTypeOptionResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.service.BoardingService;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.room.entity.RoomType;
import com.astral.express.pccms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardingBookingUseCaseTest {

    @Mock
    private BoardingService boardingService;

    @Mock
    private BoardingBookingRepository boardingBookingRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private AppointmentResponseAssembler assembler;

    @InjectMocks
    private BoardingBookingUseCase useCase;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
    }

    @Test
    void should_CreateBoardingBooking_Successfully() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        CreateBoardingBookingRequest request = new CreateBoardingBookingRequest(
                petId, roomTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Need care"
        );
        UUID bookingId = UUID.randomUUID();
        com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse dedicatedResponse =
                org.mockito.Mockito.mock(com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse.class);
        given(dedicatedResponse.id()).willReturn(bookingId);
        given(boardingService.createBooking(any())).willReturn(dedicatedResponse);

        BoardingBooking booking = new BoardingBooking();
        booking.setId(bookingId);
        given(boardingBookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        BoardingBookingResponse expectedResponse = org.mockito.Mockito.mock(BoardingBookingResponse.class);
        given(expectedResponse.id()).willReturn(bookingId);
        given(assembler.toBoardingResponse(booking)).willReturn(expectedResponse);

        // WHEN
        BoardingBookingResponse response = useCase.createBoardingBooking(request, ownerId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(bookingId);
    }

    @Test
    void should_ThrowException_when_CreateBoardingBooking_ButNotFoundAfterCreation() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        CreateBoardingBookingRequest request = new CreateBoardingBookingRequest(
                petId, roomTypeId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Need care"
        );
        UUID bookingId = UUID.randomUUID();
        com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse dedicatedResponse =
                org.mockito.Mockito.mock(com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse.class);
        given(dedicatedResponse.id()).willReturn(bookingId);
        given(boardingService.createBooking(any())).willReturn(dedicatedResponse);
        given(boardingBookingRepository.findById(bookingId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> useCase.createBoardingBooking(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_001_NOT_FOUND);
    }

    @Test
    void should_ReturnOwnerBoardingBookings() {
        // GIVEN
        BoardingBooking booking = new BoardingBooking();
        booking.setId(UUID.randomUUID());
        given(boardingBookingRepository.findByOwnerId(ownerId)).willReturn(List.of(booking));

        BoardingBookingResponse expectedResponse = org.mockito.Mockito.mock(BoardingBookingResponse.class);
        given(expectedResponse.id()).willReturn(booking.getId());
        given(assembler.toBoardingResponse(booking)).willReturn(expectedResponse);

        // WHEN
        List<BoardingBookingResponse> responses = useCase.listOwnerBoardingBookings(ownerId);

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(booking.getId());
    }

    @Test
    void should_ReturnActiveRoomTypes() {
        // GIVEN
        RoomType type = new RoomType();
        type.setId(UUID.randomUUID());
        given(roomTypeRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(List.of(type));

        RoomTypeOptionResponse expectedResponse = new RoomTypeOptionResponse(
                type.getId(), "VIP", "VIP Room", null
        );
        given(assembler.toRoomTypeOptionResponse(type)).willReturn(expectedResponse);

        // WHEN
        List<RoomTypeOptionResponse> responses = useCase.listActiveRoomTypes();

        // THEN
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(type.getId());
    }
}
